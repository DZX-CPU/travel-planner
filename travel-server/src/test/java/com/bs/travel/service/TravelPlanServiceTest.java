package com.bs.travel.service;

import com.bs.travel.ai.GeneratePlanDTO;
import com.bs.travel.common.BizException;
import com.bs.travel.entity.TravelPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TravelPlanServiceTest {

    @Test
    void peopleNullBecomesOne() {
        assertEquals(1, TravelPlanService.normalizePeople(null));
    }

    @Test
    void peopleKeepsValidValue() {
        assertEquals(4, TravelPlanService.normalizePeople(4));
    }

    @Test
    void peopleOutOfRangeThrows() {
        assertThrows(BizException.class, () -> TravelPlanService.normalizePeople(0));
        assertThrows(BizException.class, () -> TravelPlanService.normalizePeople(11));
    }

    @Test
    void arrivalTimeBlankBecomesAfternoon() {
        assertEquals("下午", TravelPlanService.normalizeArrivalTime(null));
        assertEquals("下午", TravelPlanService.normalizeArrivalTime("   "));
    }

    @Test
    void arrivalTimeKeepsValidValue() {
        assertEquals("上午", TravelPlanService.normalizeArrivalTime("上午"));
        assertEquals("晚上", TravelPlanService.normalizeArrivalTime(" 晚上 "));
    }

    @Test
    void arrivalTimeUnknownThrows() {
        assertThrows(BizException.class, () -> TravelPlanService.normalizeArrivalTime("中午"));
    }

    @Test
    void perCapitaDividesBudget() {
        assertEquals(3000, TravelPlanService.perCapita("6000", 2).intValue());
    }

    @Test
    void perCapitaRoundsDown() {
        // 6001 / 3 = 2000.33，向下取整，宁可报得保守
        assertEquals(2000, TravelPlanService.perCapita("6001", 3).intValue());
    }

    @Test
    void perCapitaIgnoresTextInBudget() {
        assertEquals(1000, TravelPlanService.perCapita("2000左右", 2).intValue());
    }

    @Test
    void perCapitaWithoutBudgetIsNull() {
        assertNull(TravelPlanService.perCapita(null, 2));
        assertNull(TravelPlanService.perCapita("  ", 2));
    }

    @Test
    void toGenerateDtoCopiesPlanFields() {
        TravelPlan plan = new TravelPlan();
        plan.setCity("成都");
        plan.setDays(3);
        plan.setPeople(2);
        plan.setArrivalTime("上午");
        plan.setBudget(new BigDecimal("6000.00"));
        plan.setPreference("美食");

        GeneratePlanDTO dto = TravelPlanService.toGenerateDto(plan);
        assertEquals("成都", dto.getCity());
        assertEquals(3, dto.getDays());
        assertEquals(2, dto.getPeople());
        assertEquals("上午", dto.getArrivalTime());
        // 6000.00 要还原成干净的数字串，再交给 parseBudget
        assertEquals("6000", dto.getBudget());
        assertEquals("美食", dto.getPreference());
    }

    @Test
    void toGenerateDtoWithoutBudgetIsNull() {
        TravelPlan plan = new TravelPlan();
        plan.setCity("杭州");
        plan.setDays(1);
        plan.setPeople(1);

        GeneratePlanDTO dto = TravelPlanService.toGenerateDto(plan);
        assertNull(dto.getBudget());
        assertEquals("杭州", dto.getCity());
    }
}
