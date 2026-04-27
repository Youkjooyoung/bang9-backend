package kr.bang9.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AnthropicClient {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String PATH_MESSAGES = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 1024;
    private static final int TIMEOUT_SECONDS = 30;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String defaultModel;

    public AnthropicClient(WebClient.Builder builder,
                           ObjectMapper objectMapper,
                           @Value("${claude.api-key:}") String apiKey,
                           @Value("${claude.model:claude-haiku-4-5-20251001}") String defaultModel) {
        this.webClient = builder.baseUrl(BASE_URL).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
    }

    public AnthropicMessageResult sendMessage(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("claude.api-key 가 비어있어 Claude API 호출을 건너뜁니다.");
            return AnthropicMessageResult.disabled();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", defaultModel);
        body.put("max_tokens", MAX_TOKENS);
        if (StringUtils.hasText(systemPrompt)) {
            body.put("system", systemPrompt);
        }
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", userPrompt
        )));

        try {
            String response = webClient.post()
                    .uri(PATH_MESSAGES)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(errorBody -> Mono.error(new WebClientResponseException(
                                    "Anthropic API 오류: " + errorBody,
                                    resp.statusCode().value(),
                                    resp.statusCode().toString(),
                                    null, null, null))))
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));

            return parseResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("Claude API 호출 실패 status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return AnthropicMessageResult.error("Claude API 호출 실패: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Claude API 호출 예외", ex);
            return AnthropicMessageResult.error("Claude API 호출 예외: " + ex.getMessage());
        }
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    private AnthropicMessageResult parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            StringBuilder text = new StringBuilder();
            if (content.isArray()) {
                for (JsonNode block : content) {
                    if ("text".equals(block.path("type").asText())) {
                        text.append(block.path("text").asText());
                    }
                }
            }
            JsonNode usage = root.path("usage");
            Integer inputTokens = usage.has("input_tokens") ? usage.get("input_tokens").asInt() : null;
            Integer outputTokens = usage.has("output_tokens") ? usage.get("output_tokens").asInt() : null;
            String model = root.path("model").asText(defaultModel);
            return new AnthropicMessageResult(true, text.toString(), model, inputTokens, outputTokens, null);
        } catch (Exception ex) {
            log.error("Claude API 응답 파싱 실패", ex);
            return AnthropicMessageResult.error("Claude API 응답 파싱 실패");
        }
    }

    public record AnthropicMessageResult(
            boolean success,
            String text,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            String errorMessage
    ) {
        public static AnthropicMessageResult disabled() {
            return new AnthropicMessageResult(false, null, null, null, null, "API 키 미설정");
        }

        public static AnthropicMessageResult error(String msg) {
            return new AnthropicMessageResult(false, null, null, null, null, msg);
        }
    }
}
