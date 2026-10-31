package com.bs.travel.ai;

import lombok.Data;

@Data
public class GeneratePlanDTO {

    private String city;        // 目的地
    private Integer days;       // 天数
    private Integer people;     // 出行人数
    private String arrivalTime; // 抵达时间：上午/下午/晚上
    private String budget;      // 预算（元，全队合计），字符串方便填"2000左右"
    private String preference;  // 偏好，逗号分隔
    private Boolean save = true; // 生成后是否直接保存
}
