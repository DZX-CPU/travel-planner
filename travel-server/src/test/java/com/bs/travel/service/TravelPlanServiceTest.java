package com.bs.travel.service;

import com.bs.travel.common.BizException;
import org.junit.jupiter.api.Test;

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
}
