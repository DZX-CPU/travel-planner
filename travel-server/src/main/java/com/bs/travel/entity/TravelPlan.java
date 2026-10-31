package com.bs.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("travel_plan")
public class TravelPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String city;

    private Integer days;

    private Integer people;

    // 上午/下午/晚上
    private String arrivalTime;

    // 全队合计，不是人均
    private BigDecimal budget;

    // 逗号分隔，如 亲子,美食
    private String preference;

    // 0草稿 1已保存
    private Integer status;

    private String aiSummary;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
