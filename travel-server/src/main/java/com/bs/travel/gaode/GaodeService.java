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
import java.util.List;

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

        // 本地有数据就直接用（只精确匹配，避免脏城市名混进缓存）
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<ScenicSpot>()
                .eq(ScenicSpot::getCity, city)
                .like(keyword != null && !keyword.isBlank(), ScenicSpot::getName, keyword)
                .last("limit 50");
        List<ScenicSpot> local = scenicSpotMapper.selectList(qw);
        if (!local.isEmpty()) {
            return local;
        }

        return fetchFromGaode(city, keyword);
    }

    private List<ScenicSpot> fetchFromGaode(String city, String keyword) {
        String url = poiSearchUrl + "?key=" + apiKey
                + "&city=" + city
                + "&citylimit=true"
                + "&keywords=" + (keyword == null || keyword.isBlank() ? "风景名胜" : keyword)
                + "&types=110000|110101|110105|140000|140100"
                + "&offset=25"
                + "&page=1"
                + "&extensions=base";

        String resp;
        try {
            resp = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            log.error("高德接口调用失败", e);
            throw new BizException("景点数据查询失败，请稍后再试");
        }

        List<ScenicSpot> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(resp);
            if (!"1".equals(root.path("status").asText())) {
                log.warn("高德返回异常: {}", root.path("info").asText());
                throw new BizException("景点数据查询失败：" + root.path("info").asText());
            }
            JsonNode pois = root.path("pois");
            for (JsonNode poi : pois) {
                ScenicSpot spot = new ScenicSpot();
                spot.setGaodeId(poi.path("id").asText());
                String name = poi.path("name").asText();
                // 高德名称常带"西栅景区-入口"这种后缀，去掉括号和短横线后面部分太粗暴，这里只去掉空格
                spot.setName(name.replace("(", "（").replace(")", "）"));
                spot.setCity(city);
                spot.setAddress(poi.path("address").asText(null));

                // 高德v3的location是"lng,lat"逗号分隔的字符串
                JsonNode loc = poi.path("location");
                String lngLat = loc.asText("");
                if (lngLat.contains(",")) {
                    String[] parts = lngLat.split(",");
                    spot.setLongitude(new BigDecimal(parts[0]));
                    spot.setLatitude(new BigDecimal(parts[1]));
                } else {
                    // 没坐标的没法在地图标点，直接丢弃
                    continue;
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
                    continue;
                }

                result.add(spot);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析高德返回失败", e);
            throw new BizException("景点数据解析失败");
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
