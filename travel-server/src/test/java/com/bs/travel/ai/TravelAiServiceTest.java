package com.bs.travel.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelAiServiceTest {

    private static final List<String> PLACEHOLDERS = List.of(
            "{city}", "{days}", "{people}", "{perCapita}", "{arrivalTime}", "{preference}", "{spotList}");

    private GeneratePlanDTO dto() {
        GeneratePlanDTO dto = new GeneratePlanDTO();
        dto.setCity("成都");
        dto.setDays(3);
        dto.setPeople(2);
        dto.setArrivalTime("下午");
        dto.setPreference("美食");
        dto.setBudget("6000");
        return dto;
    }

    @Test
    void noPlaceholderLeftUnfilled() {
        String prompt = TravelAiService.buildUserPrompt(dto(), 3000, "宽窄巷子|风景名胜|成都|104.05|30.66|免费");
        for (String ph : PLACEHOLDERS) {
            assertFalse(prompt.contains(ph), "占位符没替换掉: " + ph);
        }
    }

    @Test
    void promptCarriesPeopleBudgetAndArrival() {
        String prompt = TravelAiService.buildUserPrompt(dto(), 3000, "宽窄巷子");
        assertTrue(prompt.contains("2人"));
        assertTrue(prompt.contains("3000元/人"));
        assertTrue(prompt.contains("下午"));
        assertTrue(prompt.contains("宽窄巷子"));
    }

    @Test
    void promptWithoutBudgetSaysUnlimited() {
        String prompt = TravelAiService.buildUserPrompt(dto(), null, "宽窄巷子");
        assertTrue(prompt.contains("不限"));
    }

    @Test
    void promptWithTooSmallBudgetWarnsInsteadOfZero() {
        // 预算填得比人数还少时人均是 0，写成"0元/人"会带偏模型
        String prompt = TravelAiService.buildUserPrompt(dto(), 0, "宽窄巷子");
        assertTrue(prompt.contains("预算很紧"));
        assertFalse(prompt.contains("0元/人"));
    }
}
