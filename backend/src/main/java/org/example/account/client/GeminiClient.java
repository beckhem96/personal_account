package org.example.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String targetModel;

    public GeminiClient(
            @Qualifier("geminiRestClient") RestClient restClient,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.target-model}") String targetModel
    ) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.targetModel = targetModel;
    }

    public String generateContent(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        JsonNode response = restClient.post()
                .uri("/models/{targetModel}:generateContent?key={key}",targetModel, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response != null && response.has("candidates")) {
            JsonNode candidates = response.get("candidates");
            if (!candidates.isEmpty()) {
                return candidates.get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText("");
            }
        }

        log.warn("Gemini API 응답에서 텍스트를 추출할 수 없습니다");
        return "분석 결과를 생성할 수 없습니다.";
    }
}
