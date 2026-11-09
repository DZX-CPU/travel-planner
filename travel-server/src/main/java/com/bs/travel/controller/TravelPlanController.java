package com.bs.travel.controller;

import com.bs.travel.ai.GeneratePlanDTO;
import com.bs.travel.common.Result;
import com.bs.travel.common.UserContext;
import com.bs.travel.entity.PlanDay;
import com.bs.travel.entity.PlanItem;
import com.bs.travel.entity.TravelPlan;
import com.bs.travel.service.TravelPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "行程", description = "AI 生成、我的行程、详情、重新生成、删除")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanService planService;

    /**
     * AI生成行程（这个接口比较慢，前端要给个加载提示）
     */
    @Operation(summary = "AI 生成按天行程并保存")
    @PostMapping("/generate")
    public Result<TravelPlan> generate(@RequestBody GeneratePlanDTO dto) {
        return Result.ok(planService.generateAndSave(UserContext.get(), dto));
    }

    @Operation(summary = "我的行程列表")
    @GetMapping("/my")
    public Result<List<TravelPlan>> my() {
        return Result.ok(planService.listMyPlans(UserContext.get()));
    }

    /**
     * 行程详情：行程本体 + 每天的条目，一次返回方便前端渲染地图
     */
    @Operation(summary = "行程详情（本人或管理员）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        TravelPlan plan = planService.getPlanDetail(UserContext.get(), id);
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

    @Operation(summary = "重新生成行程（覆盖原内容，保留行程 id）")
    @PostMapping("/{id}/regenerate")
    public Result<TravelPlan> regenerate(@PathVariable Long id) {
        return Result.ok(planService.regenerate(UserContext.get(), id));
    }

    @Operation(summary = "删除自己的行程（同时清理行程天和行程项）")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        planService.deletePlan(UserContext.get(), id);
        return Result.ok();
    }
}
