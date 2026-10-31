package com.bs.travel.controller;

import com.bs.travel.ai.GeneratePlanDTO;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.PlanDay;
import com.bs.travel.entity.PlanItem;
import com.bs.travel.entity.TravelPlan;
import com.bs.travel.service.TravelPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanService planService;

    /**
     * AI生成行程（这个接口比较慢，前端要给个加载提示）
     */
    @PostMapping("/generate")
    public Result<TravelPlan> generate(@RequestBody GeneratePlanDTO dto) {
        return Result.ok(planService.generateAndSave(UserContext.get(), dto));
    }

    @GetMapping("/my")
    public Result<List<TravelPlan>> my() {
        return Result.ok(planService.listMyPlans(UserContext.get()));
    }

    /**
     * 行程详情：行程本体 + 每天的条目，一次返回方便前端渲染地图
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        TravelPlan plan = planService.getPlanDetail(id);
        List<PlanDay> days = planService.getPlanDays(id);
        Map<Long, List<PlanItem>> itemsByDay = new HashMap<>();
        if (!days.isEmpty()) {
            List<PlanItem> items = planService.getDayItems(days.stream().map(PlanDay::getId).toList());
            for (PlanItem item : items) {
                itemsByDay.computeIfAbsent(item.getDayId(), k -> new java.util.ArrayList<>()).add(item);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("plan", plan);
        data.put("days", days);
        data.put("itemsByDay", itemsByDay);
        return Result.ok(data);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        planService.deletePlan(UserContext.get(), id);
        return Result.ok();
    }
}
