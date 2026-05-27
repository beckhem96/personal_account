package org.example.account.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.account.client.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiCategoryClassifier {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key:}")
    private String apiKey;

    public Map<String, String> classify(Collection<String> merchants, List<String> categoryNames) {
        if (merchants == null || merchants.isEmpty() || categoryNames == null || categoryNames.isEmpty()) {
            return Collections.emptyMap();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Gemini API 키가 설정되지 않아 카테고리 자동 분류를 건너뜁니다.");
            return Collections.emptyMap();
        }

        String prompt = buildPrompt(merchants, categoryNames);

        try {
            String raw = geminiClient.generateContent(prompt);
            return parseResponse(raw, categoryNames);
        } catch (Exception e) {
            log.warn("Gemini 카테고리 분류 호출 실패 — 미분류로 대체합니다. cause={}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String buildPrompt(Collection<String> merchants, List<String> categoryNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 한국 가계부의 가맹점명을 카테고리로 분류하는 도우미입니다.\n");
        sb.append("아래 가맹점을 반드시 다음 카테고리 중 하나로만 분류하세요:\n");
        sb.append(String.join(", ", categoryNames));
        sb.append("\n\n가맹점 목록:\n");
        for (String m : merchants) {
            sb.append("- ").append(m).append("\n");
        }
        sb.append("\n응답은 반드시 가맹점명을 키로 카테고리명을 값으로 하는 JSON 객체만 출력하세요. ");
        sb.append("코드 블록(```)이나 부가 설명 없이 순수 JSON만 출력합니다. ");
        sb.append("예: {\"스타벅스\": \"식비\", \"GS25\": \"식비\"}");
        return sb.toString();
    }

    private Map<String, String> parseResponse(String raw, List<String> validCategories) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        String cleaned = stripCodeFence(raw);
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            if (!node.isObject()) {
                return Collections.emptyMap();
            }
            Map<String, String> result = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String category = entry.getValue().asText("").trim();
                if (validCategories.contains(category)) {
                    result.put(entry.getKey(), category);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Gemini 응답 JSON 파싱 실패: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String stripCodeFence(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline > 0) {
                t = t.substring(firstNewline + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }
}
