package com.bs.travel.gaode;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bs.travel.common.BizException;
import com.bs.travel.entity.ScenicSpot;
import com.bs.travel.mapper.ScenicSpotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 高德POI搜索，查到的结果存scenic_spot表当缓存，避免重复消耗配额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GaodeService {

    @Value("${gaode.web-service-key:}")
    private String apiKey;

    @Value("${gaode.poi-search-url}")
    private String poiSearchUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ScenicSpotMapper scenicSpotMapper;

    // 高德单页最多25条，翻两页就够用；再往后相关性明显变差，反而会把远郊冷门景点塞给模型
    private static final int PAGE_SIZE = 25;
    private static final int PAGES = 2;

    // 候选池少于这个数就认为没拉全，补拉一次
    private static final int MIN_POOL = 40;

    // 本地一次最多取这么多条喂给模型，给管理员手工补录的景点留出空间
    private static final int LOCAL_LIMIT = 60;

    // 已经补拉过的城市。没有这个集合，景点本来就少的城市每次生成行程都会重新打高德
    private final Set<String> refilledCities = ConcurrentHashMap.newKeySet();

    /**
     * 按城市搜景点，先查本地库，没有再调高德并落库
     */
    public List<ScenicSpot> searchSpots(String city, String keyword) {
        // 城市名先过一道校验，防止怪输入被存进库
        if (city == null || city.isBlank()) {
            throw new BizException("请填写城市");
        }
        if (city.length() > 20 || !city.matches("[\\u4e00-\\u9fa5a-zA-Z]{2,20}")) {
            throw new BizException("请选择正确的城市名称");
        }

        boolean byKeyword = keyword != null && !keyword.isBlank();
        List<ScenicSpot> local = queryLocal(city, keyword);

        if (!byKeyword) {
            // 行程生成走这条：池子不够就把高德翻两页补进来，一个城市只补一次
            if (local.size() < MIN_POOL && !refilledCities.contains(city)) {
                fetchFromGaode(city, null);
                refilledCities.add(city);
                local = queryLocal(city, null);
            }
            return local;
        }

        // 按关键词搜：本地有就直接用，没有再实时查高德
        return local.isEmpty() ? fetchFromGaode(city, keyword) : local;
    }

    private List<ScenicSpot> queryLocal(String city, String keyword) {
        // 只精确匹配城市，避免脏城市名混进缓存
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<ScenicSpot>()
                .eq(ScenicSpot::getCity, city)
                .like(keyword != null && !keyword.isBlank(), ScenicSpot::getName, keyword)
                .last("limit " + LOCAL_LIMIT);
        return scenicSpotMapper.selectList(qw);
    }

    private List<ScenicSpot> fetchFromGaode(String city, String keyword) {
        List<ScenicSpot> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int page = 1; page <= PAGES; page++) {
            JsonNode pois;
            try {
                pois = requestPage(city, keyword, page);
            } catch (BizException e) {
                // 第一页就拿不到说明这城市查不了；后面几页失败就用已经拿到的，不影响本次生成
                if (page == 1) {
                    throw e;
                }
                log.warn("第{}页景点拉取失败，先用前{}页结果: {}", page, page - 1, e.getMessage());
                break;
            }
            // 高德没有更多数据时返回空数组，后面几页不用再打
            if (pois.isEmpty()) {
                break;
            }

            for (JsonNode poi : pois) {
                ScenicSpot spot = toSpot(poi, city);
                if (spot == null) {
                    continue;
                }
                // 翻页时高德偶尔返回重复POI，同一个gaode_id只收一次
                String gaodeId = spot.getGaodeId();
                if (gaodeId != null && !gaodeId.isBlank() && !seen.add(gaodeId)) {
                    continue;
                }
                result.add(spot);
            }
        }

        // 落库缓存，gaode_id唯一键冲突的跳过
        for (ScenicSpot spot : result) {
            try {
                scenicSpotMapper.insert(spot);
            } catch (DuplicateKeyException e) {
                log.debug("spot已存在，跳过: {}", spot.getName());
            }
        }
        return result;
    }

    /**
     * 调一页高德POI搜索，返回pois数组。城市无效、配额超限这些都抛BizException
     */
    private JsonNode requestPage(String city, String keyword, int page) {
        String url = poiSearchUrl + "?key=" + apiKey
                + "&city=" + city
                + "&citylimit=true"
                + "&keywords=" + (keyword == null || keyword.isBlank() ? "风景名胜" : keyword)
                + "&types=110000|110101|110105|140000|140100"
                + "&offset=" + PAGE_SIZE
                + "&page=" + page
                + "&extensions=base";

        String resp;
        try {
            resp = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("高德接口调用失败", e);
            throw new BizException("景点数据查询失败，请稍后再试");
        }

        try {
            JsonNode root = objectMapper.readTree(resp);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("高德返回异常: {}", root.path("info").asText());
                throw new BizException("景点数据查询失败：" + root.path("info").asText());
            }
            return root.path("pois");
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析高德返回失败", e);
            throw new BizException("景点数据解析失败");
        }
    }

    /**
     * 单条POI转景点对象。没坐标或者城市对不上的返回null，由调用方丢弃
     */
    private ScenicSpot toSpot(JsonNode poi, String city) {
        ScenicSpot spot = new ScenicSpot();
        spot.setGaodeId(poi.path("id").asText());
        String name = poi.path("name").asText();
        // 高德名称常带"西栅景区-入口"这种后缀，去掉括号和短横线后面部分太粗暴，这里只把括号统一成全角
        spot.setName(name.replace("(", "（").replace(")", "）"));
        spot.setCity(city);
        spot.setAddress(poi.path("address").asText(null));

        // 高德v3的location是"lng,lat"逗号分隔的字符串
        String lngLat = poi.path("location").asText("");
        if (!lngLat.contains(",")) {
            // 没坐标的没法在地图标点，直接丢弃
            return null;
        }
        try {
            String[] parts = lngLat.split(",");
            spot.setLongitude(new BigDecimal(parts[0].trim()));
            spot.setLatitude(new BigDecimal(parts[1].trim()));
        } catch (RuntimeException e) {
            log.debug("坐标格式异常，跳过: {}", name);
            return null;
        }

        // type形如"科教文化服务;博物馆;科技馆"，取第二段
        String type = poi.path("type").asText("");
        String[] typeParts = type.split(";");
        spot.setType(typeParts.length > 1 ? typeParts[1] : typeParts[0]);

        // 高德在city参数无效时会返回默认城市的POI，城市对不上就丢弃，
        // 否则故宫会被存成"xyzabc的景点"。名称/地址含城市名也算过
        if (!cityCheck(poi, city)
                && !name.contains(city)
                && !(spot.getAddress() != null && spot.getAddress().contains(city))) {
            return null;
        }
        return spot;
    }

    /**
     * 高德的部分POI有pname/cityname字段，检查POI归属城市和请求的城市是否一致
     * 先看返回里有没有cityname这类字段，没有的话靠pname兜底
     */
    private boolean cityCheck(JsonNode poi, String city) {
        String[] fields = {"cityname", "pname"};
        for (String f : fields) {
            String v = poi.path(f).asText("");
            if (!v.isEmpty()) {
                // 高德会带“杭州市”这类后缀，去掉“市”再比
                return v.replace("市", "").equals(city.replace("市", ""));
            }
        }
        return false;
    }
}
