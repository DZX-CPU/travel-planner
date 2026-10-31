package com.bs.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scenic_spot")
public class ScenicSpot {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 高德POI的id，用来去重
    private String gaodeId;

    private String name;

    private String city;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    // 风景名胜/公园/博物馆这类
    private String type;

    private BigDecimal ticketPrice;

    private BigDecimal rating;

    private String imageUrl;

    private String description;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
