ALTER TABLE `travel_plan`
    ADD COLUMN `people` INT NOT NULL DEFAULT 1 COMMENT '出行人数' AFTER `days`,
    ADD COLUMN `arrival_time` VARCHAR(10) DEFAULT NULL COMMENT '抵达时间: 上午/下午/晚上' AFTER `people`;

ALTER TABLE `travel_plan`
    MODIFY COLUMN `budget` DECIMAL(10,2) DEFAULT NULL COMMENT '总预算(元, 全队合计)';
