package com.bs.travel.ai;

import com.bs.travel.common.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

/**
 * 行程生成核心服务：
 * 1. 先从景点库捞候选景点（RAG思想，用真实POI约束模型别瞎编）
 * 2. 拼Prompt调LLM，要求返回固定格式的JSON
 * 3. 解析失败重试一次
 */
@Slf4j
@Service
public class TravelAiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // 让模型按攻略博主的口吻写，别一口一个"为您推荐"
    private static final String SYSTEM_PROMPT = """
            你是一位资深的旅行规划师，常年混迹各大旅游论坛，说话接地气，像一个热心的本地朋友在帮你排行程。
            你的任务是根据用户的需求和给定的候选景点列表，生成一份按天划分的旅行行程。

            要求：
            1. 只能从候选景点列表中挑选地点，可以合理调整顺序和取舍，但绝对不要编造列表里没有的景点
            2. 每天的安排要合理：景点不要排太满，上午精力充沛排大景点，下午可以轻松些，用餐时间固定在11:30-13:00和17:30-19:00左右
            3. 同一天的景点尽量在地理上顺路，不要来回跑
            4. description用口语化的短句写实用建议（怎么去、玩什么、注意什么），禁止出现"为您推荐"、"希望对您有帮助"、"祝您旅途愉快"这类客服腔
            5. cost填单人预估花费（元）。门票、餐饮按人头算；住宿按整间房的价格除以人数，填每人分摊的那份；打车、包车按人分摊。加总后的单人合计尽量不超过人均预算
            6. 第一天是抵达日，节奏要放松，不排硬性打卡点，宁可少去一个地方也不要赶
            7. 第一天到了之后必须先去住处放行李，放完在住处附近吃一顿饭。第一天从几点开始排，按抵达时间来：上午到就从午饭前后开始，下午到只排傍晚的晚饭和住处附近走走，晚上到只安排住处附近的夜宵然后早点休息
            8. 住处全程住同一家，只在第一天写一条 activity 为"住宿"的条目，spotName 填住处名。第二天起不再重复写住宿条目，默认从住处出发
            9. 经纬度只填候选景点列表里地点的，其他条目（用餐、交通、住宿、购物、自由活动）的 longitude 和 latitude 一律填 null，不要编造坐标
            10. 严格按照指定的JSON格式输出，不要输出任何JSON以外的内容
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            请为我规划一次旅行，需求如下：
            - 目的地城市：{city}
            - 天数：{days}天
            - 出行人数：{people}人
            - 人均预算：{perCapita}
            - 抵达时间：{arrivalTime}
            - 偏好：{preference}

            候选景点列表（名称|类型|地址|经度|纬度|门票）：
            {spotList}

            请严格按照下面的JSON结构输出：
            {
              "title": "行程标题，起个有意思的名字",
              "summary": "两三句话的整体介绍，口语化",
              "days": [
                {
                  "dayNo": 1,
                  "theme": "当天主题",
                  "items": [
                    {
                      "startTime": "09:00",
                      "endTime": "11:30",
                      "spotName": "地点名称，是景点的必须与候选列表一致",
                      "longitude": 120.123456,
                      "latitude": 30.123456,
                      "activity": "景点/用餐/交通/住宿/购物/自由活动 之一",
                      "description": "口语化的实用建议，50字以内",
                      "cost": 80.0
                    }
                  ]
                }
              ]
            }
            注意：不是候选列表里的地点（餐厅、酒店、交通工具这些），longitude 和 latitude 填 null。
            """;

    public TravelAiService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * 把模板里的占位符填上，漏填一个模型就会收到带大括号的原文
     */
    static String buildUserPrompt(GeneratePlanDTO dto, Integer perCapita, String spotListText) {
        return USER_PROMPT_TEMPLATE
                .replace("{city}", dto.getCity())
                .replace("{days}", String.valueOf(dto.getDays()))
                .replace("{people}", String.valueOf(dto.getPeople()))
                .replace("{perCapita}", describePerCapita(perCapita))
                .replace("{arrivalTime}", dto.getArrivalTime())
                .replace("{preference}", dto.getPreference() == null || dto.getPreference().isBlank()
                        ? "无特别偏好" : dto.getPreference())
                .replace("{spotList}", spotListText);
    }

    // 人均算出来是 0，说明预算填得比人数还少。直接写"0元/人"会让模型往离谱的方向排
    private static String describePerCapita(Integer perCapita) {
        if (perCapita == null) {
            return "不限，尽量经济";
        }
        if (perCapita <= 0) {
            return "预算很紧，尽量挑免费或者便宜的去处";
        }
        return perCapita + "元/人";
    }

    public AiPlanResult generatePlan(GeneratePlanDTO dto, Integer perCapita, String spotListText) {
        String userPrompt = buildUserPrompt(dto, perCapita, spotListText);

        // 最多试两次，模型偶尔会输出不合法的JSON
        for (int i = 0; i < 2; i++) {
            try {
                String content = chatClient.prompt()
                        .options(OpenAiChatOptions.builder()
                                .withModel("deepseek-flash")
                                .withTemperature(0.7)
                                .build())
                        .system(SYSTEM_PROMPT)
                        .user(userPrompt)
                        .call()
                        .content();

                return parsePlan(content);
            } catch (Exception e) {
                log.warn("第{}次生成行程解析失败: {}", i + 1, e.getMessage());
                if (i == 1) {
                    throw new BizException("AI生成行程失败，请重试一下");
                }
            }
        }
        throw new BizException("AI生成行程失败，请重试一下");
    }

    private AiPlanResult parsePlan(String content) throws Exception {
        // 模型有时会包个```json代码块，剥掉
        String json = content.trim();
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start + 1, end).trim();
            }
        }
        AiPlanResult result = objectMapper.readValue(json, AiPlanResult.class);

        if (result.getDays() == null || result.getDays().isEmpty()) {
            throw new IllegalStateException("days为空");
        }
        return result;
    }
}
