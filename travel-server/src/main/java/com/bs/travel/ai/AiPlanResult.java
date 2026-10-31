package com.bs.travel.ai;

import lombok.Data;

import java.util.List;

/**
 * LLM 返回的行程 JSON 结构，前端和落库都按这个来
 */
@Data
public class AiPlanResult {

    private String title;
    private String summary;
    private List<DayPlan> days;

    @Data
    public static class DayPlan {
        private Integer dayNo;
        private String theme;
        private List<Item> items;
    }

    @Data
    public static class Item {
        private String startTime;   // "09:00"
        private String endTime;
        private String spotName;
        private Double longitude;
        private Double latitude;
        private String activity;    // 景点/用餐/交通/住宿/购物/自由活动
        private String description;
        private Double cost;
    }
}
