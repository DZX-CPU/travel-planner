package com.bs.travel.ai;

import com.bs.travel.entity.ScenicSpot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 把 AI 返回的结果收拾成能落库的样子。都是纯计算，不碰数据库
 */
public class AiPlanNormalizer {

    // 只有景点条目才补坐标，用餐、住宿这些留空，前端不在地图上打点
    private static final String SPOT_ACTIVITY = "景点";

    // 括号里的补充说明、横线后面的入口/分店后缀，都不算景点名的一部分
    private static final Pattern DECORATION =
            Pattern.compile("[（(\\[【].*?[）)\\]】]|[-－–—|/].*$");

    /**
     * 坐标只信景点库里的。名称对得上的用库里的经纬度覆盖模型给的，
     * 对不上的（餐厅、酒店这些）直接置空不打点。
     * 模型经常把旁边景点的坐标顺手抄给餐厅，光靠Prompt拦不住
     */
    public static void fixCoordinates(AiPlanResult result, List<ScenicSpot> spots) {
        Map<String, ScenicSpot> byName = new HashMap<>();
        // 去掉括号后缀后的名字 -> 景点，同一个核心名对应多个景点时视为有歧义
        Map<String, List<ScenicSpot>> byCoreName = new HashMap<>();

        for (ScenicSpot spot : spots) {
            if (spot.getName() == null || spot.getName().isBlank()) {
                continue;
            }
            String name = spot.getName().trim();
            byName.put(name, spot);
            byCoreName.computeIfAbsent(coreName(name), k -> new ArrayList<>()).add(spot);
        }

        for (AiPlanResult.DayPlan day : result.getDays()) {
            if (day.getItems() == null) {
                continue;
            }
            for (AiPlanResult.Item item : day.getItems()) {
                ScenicSpot matched = match(item, byName, byCoreName, spots);
                if (matched == null) {
                    item.setLongitude(null);
                    item.setLatitude(null);
                    continue;
                }
                item.setLongitude(matched.getLongitude() == null ? null : matched.getLongitude().doubleValue());
                item.setLatitude(matched.getLatitude() == null ? null : matched.getLatitude().doubleValue());
            }
        }
    }

    /**
     * 按可信度从高到低找景点：
     * 1. 名字完全一样，直接用
     * 2. 去掉括号/横线后缀后一样，用（"灵隐寺(飞来峰景区)" 能对上 "灵隐寺"）
     * 3. 库里名字更完整、包含模型写的名字，且候选唯一（"河坊街" 对上 "河坊街步行街"）
     * 都对不上就返回 null，宁可地图少一个点，也不把点标到别的景点上
     */
    private static ScenicSpot match(AiPlanResult.Item item, Map<String, ScenicSpot> byName,
                                    Map<String, List<ScenicSpot>> byCoreName, List<ScenicSpot> spots) {
        if (item.getSpotName() == null) {
            return null;
        }
        String name = item.getSpotName().trim();
        if (name.isEmpty()) {
            return null;
        }

        ScenicSpot exact = byName.get(name);
        if (exact != null) {
            return exact;
        }

        // 后面两步是模糊匹配，只给景点用：用餐、住宿名字里常常带地标，
        // 放开了会把一顿饭标到景点坐标上
        if (!SPOT_ACTIVITY.equals(item.getActivity())) {
            return null;
        }

        String core = coreName(name);
        if (core.length() < 2) {
            return null;
        }

        List<ScenicSpot> sameCore = byCoreName.get(core);
        if (sameCore != null && sameCore.size() == 1) {
            return sameCore.get(0);
        }

        // 只认"库里更长"这一个方向。"西湖音乐喷泉"对"西湖"这种反向包含不能算命中，
        // 否则会把喷泉的坐标安到一个叫西湖的条目上
        ScenicSpot contained = null;
        for (ScenicSpot spot : spots) {
            if (spot.getName() == null || spot.getName().isBlank()) {
                continue;
            }
            if (!coreName(spot.getName()).contains(core)) {
                continue;
            }
            if (contained != null) {
                return null;
            }
            contained = spot;
        }
        return contained;
    }

    // 去掉括号说明和横线后缀，取名字主干。整段都被去掉了就退回原名
    private static String coreName(String name) {
        String core = DECORATION.matcher(name).replaceAll("").trim();
        return core.isEmpty() ? name.trim() : core;
    }

    // 兜底补的住宿条目按抵达时间挑时段
    private static final Map<String, String[]> STAY_SLOT = Map.of(
            "上午", new String[]{"11:30", "12:00"},
            "下午", new String[]{"17:00", "17:30"},
            "晚上", new String[]{"20:00", "20:30"}
    );

    /**
     * 第一天必须先去住处放行李。模型偶尔会漏，漏了就补一条
     */
    public static void ensureFirstDayStay(AiPlanResult result, String arrivalTime) {
        AiPlanResult.DayPlan firstDay = result.getDays().get(0);
        if (firstDay.getItems() == null) {
            firstDay.setItems(new ArrayList<>());
        }
        List<AiPlanResult.Item> items = firstDay.getItems();
        for (AiPlanResult.Item item : items) {
            if ("住宿".equals(item.getActivity())) {
                return;
            }
        }

        String[] slot = STAY_SLOT.getOrDefault(arrivalTime == null ? "" : arrivalTime, STAY_SLOT.get("下午"));
        AiPlanResult.Item stay = new AiPlanResult.Item();
        stay.setSpotName("住处（先放行李）");
        stay.setActivity("住宿");
        stay.setDescription("先去住的地方把行李放下，轻装出门，顺便缓一缓");
        stay.setStartTime(slot[0]);
        stay.setEndTime(slot[1]);

        // 按开始时间找位置，插到第一个比住宿更晚的条目前面，时间轴就不会倒序
        int stayAt = minutesOf(slot[0]);
        int index = items.size();
        for (int i = 0; i < items.size(); i++) {
            int itemAt = minutesOf(items.get(i).getStartTime());
            if (itemAt > stayAt) {
                index = i;
                break;
            }
        }
        items.add(index, stay);
    }

    // 时间转成分钟数，认不出来返回 -1。模型偶尔会写成"9:00"，不能直接比字符串
    private static int minutesOf(String time) {
        if (time == null) {
            return -1;
        }
        String[] parts = time.trim().split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
