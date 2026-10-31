package com.bs.travel.ai;

import com.bs.travel.entity.ScenicSpot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 AI 返回的结果收拾成能落库的样子。都是纯计算，不碰数据库
 */
public class AiPlanNormalizer {

    /**
     * 坐标只信景点库里的。名称对得上的用库里的经纬度覆盖模型给的，
     * 对不上的（餐厅、酒店这些）直接置空不打点。
     * 模型经常把旁边景点的坐标顺手抄给餐厅，光靠Prompt拦不住
     */
    public static void fixCoordinates(AiPlanResult result, List<ScenicSpot> spots) {
        Map<String, ScenicSpot> byName = new HashMap<>();
        for (ScenicSpot spot : spots) {
            if (spot.getName() != null) {
                byName.put(spot.getName().trim(), spot);
            }
        }

        for (AiPlanResult.DayPlan day : result.getDays()) {
            if (day.getItems() == null) {
                continue;
            }
            for (AiPlanResult.Item item : day.getItems()) {
                ScenicSpot matched = item.getSpotName() == null
                        ? null
                        : byName.get(item.getSpotName().trim());
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
