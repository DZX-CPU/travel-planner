-- ============================================================
-- 基于大语言模型的智慧文旅行程规划系统 - 数据库初始化脚本
-- ============================================================
CREATE DATABASE IF NOT EXISTS travel_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(SHA-256加盐)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `role`        TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0-普通用户 1-管理员',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='用户表';

-- 景点表（高德POI缓存）
CREATE TABLE IF NOT EXISTS `scenic_spot` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '景点ID',
    `gaode_id`    VARCHAR(64)   DEFAULT NULL COMMENT '高德POI ID',
    `name`        VARCHAR(100)  NOT NULL COMMENT '景点名称',
    `city`        VARCHAR(50)   NOT NULL COMMENT '所在城市',
    `address`     VARCHAR(255)  DEFAULT NULL COMMENT '地址',
    `longitude`   DECIMAL(10,6) NOT NULL COMMENT '经度',
    `latitude`    DECIMAL(10,6) NOT NULL COMMENT '纬度',
    `type`        VARCHAR(50)   DEFAULT NULL COMMENT '类型(风景名胜/公园/博物馆等)',
    `ticket_price` DECIMAL(10,2) DEFAULT NULL COMMENT '门票价格(元)',
    `rating`      DECIMAL(2,1)  DEFAULT NULL COMMENT '评分',
    `image_url`   VARCHAR(255)  DEFAULT NULL COMMENT '图片URL',
    `description` TEXT          DEFAULT NULL COMMENT '简介',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_gaode_id` (`gaode_id`),
    KEY `idx_city` (`city`)
) ENGINE=InnoDB COMMENT='景点表';

-- 行程表
CREATE TABLE IF NOT EXISTS `travel_plan` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '行程ID',
    `user_id`        BIGINT        NOT NULL COMMENT '所属用户',
    `title`          VARCHAR(100)  NOT NULL COMMENT '行程标题',
    `city`           VARCHAR(50)   NOT NULL COMMENT '目的地城市',
    `days`           INT           NOT NULL COMMENT '天数',
    `people`         INT           NOT NULL DEFAULT 1 COMMENT '出行人数',
    `arrival_time`   VARCHAR(10)   DEFAULT NULL COMMENT '抵达时间: 上午/下午/晚上',
    `budget`         DECIMAL(10,2) DEFAULT NULL COMMENT '总预算(元, 全队合计)',
    `preference`     VARCHAR(255)  DEFAULT NULL COMMENT '偏好(亲子/美食/文艺等,逗号分隔)',
    `status`         TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-已保存',
    `ai_summary`     TEXT          DEFAULT NULL COMMENT 'AI 生成总结',
    `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='行程表';

-- 行程天表
CREATE TABLE IF NOT EXISTS `plan_day` (
    `id`          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '天ID',
    `plan_id`     BIGINT  NOT NULL COMMENT '所属行程',
    `day_no`      INT     NOT NULL COMMENT '第几天(从1开始)',
    `theme`       VARCHAR(100) DEFAULT NULL COMMENT '当日主题',
    `remark`      VARCHAR(255) DEFAULT NULL COMMENT '当日备注',
    PRIMARY KEY (`id`),
    KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB COMMENT='行程天表';

-- 行程条目表
CREATE TABLE IF NOT EXISTS `plan_item` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '条目ID',
    `day_id`      BIGINT        NOT NULL COMMENT '所属天',
    `sort_no`     INT           NOT NULL DEFAULT 0 COMMENT '当日顺序',
    `start_time`  VARCHAR(20)   DEFAULT NULL COMMENT '开始时间(如 09:00)',
    `end_time`    VARCHAR(20)   DEFAULT NULL COMMENT '结束时间',
    `spot_name`   VARCHAR(100)  NOT NULL COMMENT '地点名称',
    `longitude`   DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    `latitude`    DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    `activity`    VARCHAR(20)   DEFAULT NULL COMMENT '活动类型(景点/用餐/交通/住宿/购物)',
    `description` VARCHAR(500)  DEFAULT NULL COMMENT '描述/建议',
    `cost`        DECIMAL(10,2) DEFAULT NULL COMMENT '预估花费(元)',
    PRIMARY KEY (`id`),
    KEY `idx_day_id` (`day_id`)
) ENGINE=InnoDB COMMENT='行程条目表';

-- 游记表（可选加分模块）
CREATE TABLE IF NOT EXISTS `travel_note` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '游记ID',
    `user_id`     BIGINT       NOT NULL COMMENT '作者',
    `title`        VARCHAR(100) NOT NULL COMMENT '标题',
    `content`     TEXT         NOT NULL COMMENT '内容',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图',
    `city`        VARCHAR(50)  DEFAULT NULL COMMENT '相关城市',
    `view_count`  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='游记表';

-- AI 对话记录表（多轮修改上下文落库，Redis 只做会话级缓存）
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户',
    `plan_id`     BIGINT      DEFAULT NULL COMMENT '关联行程',
    `role`        VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    `content`     TEXT        NOT NULL COMMENT '内容',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_plan` (`user_id`, `plan_id`)
) ENGINE=InnoDB COMMENT='AI对话记录表';

-- 初始管理员账号: admin / admin123（SHA-256加盐，格式: 盐$摘要）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`)
VALUES ('admin', 'rGVqyl97R6BmU651BbaT+Q==$9285c4141e7bdab7299dcf5e0c0beca1298279e391631a0ab9f90bcf9c9ec73a', '管理员', 1);
