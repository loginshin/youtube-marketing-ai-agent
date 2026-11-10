package com.marketing.agent.ai;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/** Server-side OpenAI transport. Does not call the API during startup. */
@Service
public class OpenAiClient {
    private final RestClient client;
    private final String apiKey;

    public OpenAiClient(@Value("${agent.ai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1")
                .requestFactory(factory).build();
    }

    public String generate(ModelTier model, String instructions, String input) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("backend/.env에 OPENAI_API_KEY를 설정하세요.");
        }
        Response response = client.post().uri("/responses")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .body(Map.of("model", model.modelId(), "instructions", instructions,
                        "input", input, "store", false, "max_output_tokens", 1000,
                        "reasoning", Map.of("effort", model.reasoningEffort())))
                .retrieve().body(Response.class);
        if (response == null || !"completed".equals(response.status()) || response.output() == null) {
            throw new IllegalStateException("AI 응답이 완료되지 않았습니다.");
        }
        StringBuilder text = new StringBuilder();
        for (Output output : response.output()) {
            if (output.content() == null) continue;
            for (Content content : output.content()) {
                if ("output_text".equals(content.type()) && content.text() != null) {
                    text.append(content.text());
                }
            }
        }
        if (text.isEmpty()) throw new IllegalStateException("AI 텍스트 응답이 없습니다.");
        return text.toString();
    }

    public record Response(String status, List<Output> output) {}
    public record Output(List<Content> content) {}
    public record Content(String type, String text) {}
}
