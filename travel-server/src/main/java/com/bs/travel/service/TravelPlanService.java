package com.bs.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bs.travel.ai.AiPlanNormalizer;
import com.bs.travel.ai.AiPlanResult;
import com.bs.travel.ai.GeneratePlanDTO;
import com.bs.travel.ai.TravelAiService;
import com.bs.travel.common.BizException;
import com.bs.travel.entity.PlanDay;
import com.bs.travel.entity.PlanItem;
import com.bs.travel.entity.ScenicSpot;
import com.bs.travel.entity.TravelPlan;
import com.bs.travel.gaode.GaodeService;
import com.bs.travel.mapper.PlanDayMapper;
import com.bs.travel.mapper.PlanItemMapper;
import com.bs.travel.mapper.TravelPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 行程的生成、查询、修改、删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private final TravelAiService travelAiService;
    private final GaodeService gaodeService;
    private final TravelPlanMapper planMapper;
    private final PlanDayMapper dayMapper;
    private final PlanItemMapper itemMapper;

    /**
     * AI生成行程：查景点 -> 喂给LLM -> 解析 -> 落库
     */
    @Transactional
    public TravelPlan generateAndSave(Long userId, GeneratePlanDTO dto) {
        if (dto.getCity() == null || dto.getCity().isBlank()) {
            throw new BizException("请填写目的地城市");
        }
        if (dto.getDays() == null || dto.getDays() < 1 || dto.getDays() > 7) {
            throw new BizException("天数支持1-7天");
        }

        // 人数和抵达时间归一化后写回dto，后面拼Prompt和落库都读这两个值
        dto.setPeople(normalizePeople(dto.getPeople()));
        dto.setArrivalTime(normalizeArrivalTime(dto.getArrivalTime()));
        Integer perCapitaBudget = perCapita(dto.getBudget(), dto.getPeople());

        // 1. 拉候选景点，作为LLM的参考（防止它编景点）
        List<ScenicSpot> spots = gaodeService.searchSpots(dto.getCity(), null);
        if (spots.isEmpty()) {
            throw new BizException("该城市暂时没有景点数据，换个城市试试");
        }

        StringBuilder sb = new StringBuilder();
        for (ScenicSpot s : spots) {
            sb.append(s.getName()).append("|")
                    .append(s.getType() == null ? "" : s.getType()).append("|")
                    .append(s.getAddress() == null ? "" : s.getAddress()).append("|")
                    .append(s.getLongitude()).append("|")
                    .append(s.getLatitude()).append("|")
                    .append(s.getTicketPrice() == null ? "未知" : s.getTicketPrice())
                    .append("\n");
        }

        // 2. 调LLM
        AiPlanResult aiResult = travelAiService.generatePlan(dto, perCapitaBudget, sb.toString());

        // 3. 收拾模型给的结果：坐标只信景点库，第一天必须先去放行李
        AiPlanNormalizer.fixCoordinates(aiResult, spots);
        AiPlanNormalizer.ensureFirstDayStay(aiResult, dto.getArrivalTime());

        // 4. 落库
        return savePlanFromAi(userId, dto, aiResult);
    }

    @Transactional
    public TravelPlan savePlanFromAi(Long userId, GeneratePlanDTO dto, AiPlanResult aiResult) {
        TravelPlan plan = new TravelPlan();
        plan.setUserId(userId);
        plan.setTitle(aiResult.getTitle());
        plan.setCity(dto.getCity());
        plan.setDays(dto.getDays());
        plan.setPeople(dto.getPeople());
        plan.setArrivalTime(dto.getArrivalTime());
        plan.setBudget(parseBudget(dto.getBudget()));
        plan.setPreference(dto.getPreference());
        plan.setStatus(1);
        plan.setAiSummary(aiResult.getSummary());
        planMapper.insert(plan);

        for (AiPlanResult.DayPlan dayPlan : aiResult.getDays()) {
            PlanDay day = new PlanDay();
            day.setPlanId(plan.getId());
            day.setDayNo(dayPlan.getDayNo() == null ? 0 : dayPlan.getDayNo());
            day.setTheme(dayPlan.getTheme());
            dayMapper.insert(day);

            int sortNo = 1;
            for (AiPlanResult.Item item : dayPlan.getItems()) {
                PlanItem pi = new PlanItem();
                pi.setDayId(day.getId());
                pi.setSortNo(sortNo++);
                pi.setStartTime(item.getStartTime());
                pi.setEndTime(item.getEndTime());
                pi.setSpotName(item.getSpotName());
                pi.setActivity(item.getActivity());
                pi.setDescription(item.getDescription());
                pi.setCost(item.getCost() == null ? null : BigDecimal.valueOf(item.getCost()));
                if (item.getLongitude() != null && item.getLatitude() != null) {
                    pi.setLongitude(BigDecimal.valueOf(item.getLongitude()).setScale(6, RoundingMode.HALF_UP));
                    pi.setLatitude(BigDecimal.valueOf(item.getLatitude()).setScale(6, RoundingMode.HALF_UP));
                }
                itemMapper.insert(pi);
            }
        }
        return plan;
    }

    /**
     * 行程详情（带天和条目）
     */
    public TravelPlan getPlanDetail(Long planId) {
        TravelPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BizException("行程不存在");
        }
        return plan;
    }

    public List<PlanDay> getPlanDays(Long planId) {
        return dayMapper.selectList(new LambdaQueryWrapper<PlanDay>()
                .eq(PlanDay::getPlanId, planId)
                .orderByAsc(PlanDay::getDayNo));
    }

    public List<PlanItem> getDayItems(List<Long> dayIds) {
        if (dayIds.isEmpty()) {
            return List.of();
        }
        return itemMapper.selectList(new LambdaQueryWrapper<PlanItem>()
                .in(PlanItem::getDayId, dayIds)
                .orderByAsc(PlanItem::getSortNo));
    }

    public List<TravelPlan> listMyPlans(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<TravelPlan>()
                .eq(TravelPlan::getUserId, userId)
                .orderByDesc(TravelPlan::getCreateTime));
    }

    public void deletePlan(Long userId, Long planId) {
        TravelPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BizException("行程不存在");
        }
        planMapper.deleteById(planId);
    }

    private static BigDecimal parseBudget(String budget) {
        if (budget == null || budget.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(budget.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 人数不填按 1 人算，填了就越界报错。老前端不带这个字段，不能直接判空报错
    static int normalizePeople(Integer people) {
        if (people == null) {
            return 1;
        }
        if (people < 1 || people > 10) {
            throw new BizException("出行人数支持1-10人");
        }
        return people;
    }

    // 抵达时间只认三档，不填默认下午
    static String normalizeArrivalTime(String arrivalTime) {
        if (arrivalTime == null || arrivalTime.isBlank()) {
            return "下午";
        }
        String value = arrivalTime.trim();
        if (!value.equals("上午") && !value.equals("下午") && !value.equals("晚上")) {
            throw new BizException("抵达时间只能是上午、下午或晚上");
        }
        return value;
    }

    // 总预算摊到每人，除不尽向下取整，宁可报得保守点
    static Integer perCapita(String budget, Integer people) {
        BigDecimal total = parseBudget(budget);
        if (total == null) {
            return null;
        }
        return total.divide(BigDecimal.valueOf(people), 0, RoundingMode.DOWN).intValue();
    }
}
