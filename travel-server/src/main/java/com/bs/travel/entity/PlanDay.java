package com.bs.travel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("plan_day")
public class PlanDay {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    // 第几天，从1开始
    private Integer dayNo;

    private String theme;

    private String remark;
}
