package com.bs.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bs.travel.entity.PlanItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlanItemMapper extends BaseMapper<PlanItem> {

    /**
     * 热门景点 TOP10：只统计有坐标的真实景点，按出现的行程项次数倒序。
     * 坐标是行程归一化时从景点库回填的，餐厅/酒店这类条目没有坐标，不会混进来。
     */
    @Select("""
            SELECT i.spot_name AS name, COUNT(*) AS cnt, MAX(s.city) AS city
            FROM plan_item i
            JOIN scenic_spot s ON s.name = i.spot_name AND s.deleted = 0
            WHERE i.longitude IS NOT NULL
            GROUP BY i.spot_name
            ORDER BY cnt DESC, name ASC
            LIMIT 10
            """)
    List<Map<String, Object>> selectHotSpots();
}
