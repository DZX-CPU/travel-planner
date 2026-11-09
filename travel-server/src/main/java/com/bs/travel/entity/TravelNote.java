package com.bs.travel.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("travel_note")
public class TravelNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String coverImage;

    private String city;

    private Integer viewCount;

    // 管理端列表展示作者用，不落库
    @TableField(exist = false)
    private String username;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
