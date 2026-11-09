package com.bs.travel.ai;

import com.bs.travel.entity.ScenicSpot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiPlanNormalizerTest {

    private ScenicSpot spot(String name, String lng, String lat) {
        ScenicSpot s = new ScenicSpot();
        s.setName(name);
        s.setLongitude(new BigDecimal(lng));
        s.setLatitude(new BigDecimal(lat));
        return s;
    }

    private AiPlanResult.Item item(String name, Double lng, Double lat) {
        AiPlanResult.Item i = new AiPlanResult.Item();
        i.setSpotName(name);
        i.setActivity("景点");
        i.setLongitude(lng);
        i.setLatitude(lat);
        return i;
    }

    private AiPlanResult resultOf(AiPlanResult.Item... items) {
        AiPlanResult.DayPlan day = new AiPlanResult.DayPlan();
        day.setDayNo(1);
        day.setItems(new ArrayList<>(List.of(items)));
        AiPlanResult result = new AiPlanResult();
        result.setDays(new ArrayList<>(List.of(day)));
        return result;
    }

    @Test
    void spotCoordinatesOverwrittenByLibrary() {
        // 模型抄错了坐标，用景点库里的覆盖掉
        AiPlanResult result = resultOf(item("宽窄巷子", 100.0, 20.0));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("宽窄巷子", "104.055000", "30.669000")));

        AiPlanResult.Item fixed = result.getDays().get(0).getItems().get(0);
        assertEquals(104.055, fixed.getLongitude().doubleValue());
        assertEquals(30.669, fixed.getLatitude().doubleValue());
    }

    @Test
    void restaurantCoordinatesCleared() {
        AiPlanResult result = resultOf(item("春熙路某火锅店", 104.08, 30.65));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("宽窄巷子", "104.055000", "30.669000")));

        AiPlanResult.Item fixed = result.getDays().get(0).getItems().get(0);
        assertNull(fixed.getLongitude());
        assertNull(fixed.getLatitude());
    }

    @Test
    void nameWithSpacesStillMatches() {
        AiPlanResult result = resultOf(item(" 宽窄巷子 ", null, null));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("宽窄巷子", "104.055000", "30.669000")));

        assertEquals(104.055, result.getDays().get(0).getItems().get(0).getLongitude().doubleValue());
    }

    @Test
    void nonSpotActivityWithoutMatchCleared() {
        // 自由活动如果名字对不上候选景点，一样不留坐标
        AiPlanResult.Item free = item("随便逛逛", 104.0, 30.0);
        free.setActivity("自由活动");
        AiPlanResult result = resultOf(free);

        AiPlanNormalizer.fixCoordinates(result, List.of(spot("宽窄巷子", "104.055000", "30.669000")));

        assertNull(result.getDays().get(0).getItems().get(0).getLongitude());
    }

    @Test
    void nullCoordinatesOnSpotStillFilled() {
        // 模型没给坐标但名字对得上，补上真实坐标
        AiPlanResult result = resultOf(item("宽窄巷子", null, null));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("宽窄巷子", "104.055000", "30.669000")));

        assertEquals(104.055, result.getDays().get(0).getItems().get(0).getLongitude().doubleValue());
    }

    @Test
    void longerAiNameNotMatchedToShorterLibraryName() {
        // 模型写的名字比库里更具体时不算命中，否则"西湖音乐喷泉"会被错配到"西湖"并继承假坐标
        AiPlanResult result = resultOf(item("西湖音乐喷泉", 104.0, 30.0));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("西湖", "120.150000", "30.250000")));

        assertNull(result.getDays().get(0).getItems().get(0).getLongitude());
    }

    @Test
    void bracketSuffixInAiNameStillMatches() {
        // 模型爱把括号里的补充说明带上，去掉后缀应该能对上库里的干净名字
        AiPlanResult result = resultOf(item("灵隐寺（飞来峰景区）", null, null));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("灵隐寺", "120.101000", "30.241000")));

        assertEquals(120.101, result.getDays().get(0).getItems().get(0).getLongitude().doubleValue());
    }

    @Test
    void dashSuffixInLibraryNameStillMatches() {
        // 高德的名字常带"-入口"这种后缀，模型写的是主干名
        AiPlanResult result = resultOf(item("西栅景区", null, null));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("西栅景区-入口", "120.487000", "30.746000")));

        assertEquals(120.487, result.getDays().get(0).getItems().get(0).getLongitude().doubleValue());
    }

    @Test
    void shorterNameMatchedToLongerLibraryName() {
        // 库里是完整官方名，模型写了个简称，唯一命中就认
        AiPlanResult result = resultOf(item("河坊街", null, null));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("河坊街步行街", "120.169000", "30.240000")));

        assertEquals(120.169, result.getDays().get(0).getItems().get(0).getLongitude().doubleValue());
    }

    @Test
    void ambiguousLongerNamesGiveUp() {
        // 两个景点都能包含"西湖"，分不清是哪个，宁可留空
        AiPlanResult result = resultOf(item("西湖", null, null));
        AiPlanNormalizer.fixCoordinates(result, List.of(
                spot("西湖风景名胜区", "120.150000", "30.250000"),
                spot("西湖音乐喷泉", "120.160000", "30.260000")));

        assertNull(result.getDays().get(0).getItems().get(0).getLongitude());
    }

    @Test
    void fuzzyMatchOnlyForSpotActivity() {
        // 用餐条目名字里带地标，不能因为名字像就继承景点坐标
        AiPlanResult result = resultOf(activityItem("河坊街", "用餐"));
        AiPlanNormalizer.fixCoordinates(result, List.of(spot("河坊街步行街", "120.169000", "30.240000")));

        assertNull(result.getDays().get(0).getItems().get(0).getLongitude());
    }

    private AiPlanResult.Item activityItem(String name, String activity) {
        AiPlanResult.Item i = new AiPlanResult.Item();
        i.setSpotName(name);
        i.setActivity(activity);
        return i;
    }

    @Test
    void stayAddedWhenFirstDayHasNone() {
        AiPlanResult.Item spot = activityItem("宽窄巷子", "景点");
        spot.setStartTime("20:00");
        AiPlanResult result = resultOf(spot);
        AiPlanNormalizer.ensureFirstDayStay(result, "下午");

        List<AiPlanResult.Item> items = result.getDays().get(0).getItems();
        assertEquals(2, items.size());
        assertEquals("住宿", items.get(0).getActivity());
        assertNull(items.get(0).getLongitude());
        assertEquals("17:00", items.get(0).getStartTime());
        assertEquals("17:30", items.get(0).getEndTime());
    }

    @Test
    void stayNotDuplicated() {
        AiPlanResult result = resultOf(activityItem("某某酒店", "住宿"), activityItem("宽窄巷子", "景点"));
        AiPlanNormalizer.ensureFirstDayStay(result, "下午");

        assertEquals(2, result.getDays().get(0).getItems().size());
    }

    @Test
    void stayGoesAfterArrivalTraffic() {
        // 第一条是刚到这座城市的交通，放行李排在它后面
        AiPlanResult.Item traffic = activityItem("双流机场→市区", "交通");
        traffic.setStartTime("08:00");
        AiPlanResult.Item spot = activityItem("宽窄巷子", "景点");
        spot.setStartTime("20:00");
        AiPlanResult result = resultOf(traffic, spot);
        AiPlanNormalizer.ensureFirstDayStay(result, "下午");

        List<AiPlanResult.Item> items = result.getDays().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals("交通", items.get(0).getActivity());
        assertEquals("住宿", items.get(1).getActivity());
        assertEquals("景点", items.get(2).getActivity());
    }

    @Test
    void stayGoesAfterEarlierSpotEvenWithoutTraffic() {
        // 模型漏写住宿，且当天首条就是上午的景点：住宿时段在晚上，不能插到景点前面
        AiPlanResult.Item spot = activityItem("宽窄巷子", "景点");
        spot.setStartTime("09:00");
        spot.setEndTime("11:30");
        AiPlanResult result = resultOf(spot);

        AiPlanNormalizer.ensureFirstDayStay(result, "晚上");

        List<AiPlanResult.Item> items = result.getDays().get(0).getItems();
        assertEquals(2, items.size());
        assertEquals("景点", items.get(0).getActivity());
        assertEquals("住宿", items.get(1).getActivity());
        assertEquals("20:00", items.get(1).getStartTime());
    }

    @Test
    void stayGoesBeforeLaterItemOnFirstDay() {
        // 已有条目的时间晚于放行李时段，放行李要排到它前面，而不是无脑追加到末尾
        AiPlanResult.Item dinner = activityItem("住处附近晚饭", "用餐");
        dinner.setStartTime("18:30");
        AiPlanResult result = resultOf(dinner);
        AiPlanNormalizer.ensureFirstDayStay(result, "下午");

        List<AiPlanResult.Item> items = result.getDays().get(0).getItems();
        assertEquals(2, items.size());
        assertEquals("住宿", items.get(0).getActivity());
        assertEquals("用餐", items.get(1).getActivity());
        assertEquals("17:00", items.get(0).getStartTime());
    }

    @Test
    void stayTimeFollowsArrival() {
        AiPlanResult.Item spot = activityItem("宽窄巷子", "景点");
        spot.setStartTime("21:00");
        AiPlanResult result = resultOf(spot);
        AiPlanNormalizer.ensureFirstDayStay(result, "晚上");

        assertEquals("20:00", result.getDays().get(0).getItems().get(0).getStartTime());
    }

    @Test
    void emptyFirstDayGetsStay() {
        AiPlanResult.DayPlan day = new AiPlanResult.DayPlan();
        day.setDayNo(1);
        day.setItems(new ArrayList<>());
        AiPlanResult result = new AiPlanResult();
        result.setDays(new ArrayList<>(List.of(day)));

        AiPlanNormalizer.ensureFirstDayStay(result, "上午");

        assertEquals(1, result.getDays().get(0).getItems().size());
        assertEquals("11:30", result.getDays().get(0).getItems().get(0).getStartTime());
    }

    @Test
    void stayAppendedWhenWholeDayIsTraffic() {
        AiPlanResult result = resultOf(activityItem("双流机场→市区", "交通"), activityItem("市区→酒店", "交通"));
        AiPlanNormalizer.ensureFirstDayStay(result, "下午");

        List<AiPlanResult.Item> items = result.getDays().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals("交通", items.get(0).getActivity());
        assertEquals("交通", items.get(1).getActivity());
        assertEquals("住宿", items.get(2).getActivity());
    }

    @Test
    void nullArrivalFallsBackToAfternoonSlot() {
        AiPlanResult.Item spot = activityItem("宽窄巷子", "景点");
        spot.setStartTime("20:00");
        AiPlanResult result = resultOf(spot);
        AiPlanNormalizer.ensureFirstDayStay(result, null);

        AiPlanResult.Item stay = result.getDays().get(0).getItems().get(0);
        assertEquals("住宿", stay.getActivity());
        assertEquals("17:00", stay.getStartTime());
        assertEquals("17:30", stay.getEndTime());
    }

    @Test
    void nullItemsDayGetsStay() {
        AiPlanResult.DayPlan day = new AiPlanResult.DayPlan();
        day.setDayNo(1);
        day.setItems(null);
        AiPlanResult result = new AiPlanResult();
        result.setDays(new ArrayList<>(List.of(day)));

        AiPlanNormalizer.ensureFirstDayStay(result, "晚上");

        assertEquals(1, result.getDays().get(0).getItems().size());
        assertEquals("住宿", result.getDays().get(0).getItems().get(0).getActivity());
    }
}
