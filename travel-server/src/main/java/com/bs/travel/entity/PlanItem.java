package com.bs.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("plan_item")
public class PlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dayId;

    private Integer sortNo;

    // "09:00"这种字符串，不用LocalTime，AI返回的时间不一定规范
    private String startTime;

    private String endTime;

    private String spotName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    // 景点/用餐/交通/住宿/购物
    private String activity;

    private String description;

    private BigDecimal cost;
}
