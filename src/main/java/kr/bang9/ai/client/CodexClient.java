package kr.bang9.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CodexClient {

    private static final String PATH_CHAT_COMPLETIONS = "/v1/chat/completions";
    private static final int MAX_TOKENS = 1024;
    private static final int TIMEOUT_SECONDS = 30;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String defaultModel;

    public CodexClient(
        WebClient.Builder builder,
        ObjectMapper objectMapper,
        @Value("${codex.base-url:https://api.openai.com}") String baseUrl,
        @Value("${codex.api-key:}") String apiKey,
        @Value("${codex.model:gpt-5.4-mini}") String defaultModel
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
    }

    public CodexMessageResult sendMessage(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("codex.api-key가 비어 있어 AI API 호출을 건너뜁니다.");
            return CodexMessageResult.disabled();
        }

        Map<String, Object> body = Map.of(
            "model", defaultModel,
            "max_completion_tokens", MAX_TOKENS,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            )
        );

        try {
            String response = webClient.post()
                .uri(PATH_CHAT_COMPLETIONS)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(errorBody -> Mono.error(new WebClientResponseException(
                        "AI API 오류: " + errorBody,
                        resp.statusCode().value(),
                        resp.statusCode().toString(),
                        null,
                        null,
                        null
                    ))))
                .bodyToMono(String.class)
                .block(java.time.Duration.ofSeconds(TIMEOUT_SECONDS));

            return parseResponse(response);
        } catch (WebClientResponseException ex) {
            log.error("AI API 호출 실패 status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return CodexMessageResult.error("AI API 호출 실패: " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("AI API 호출 예외", ex);
            return CodexMessageResult.error("AI API 호출 예외: " + ex.getMessage());
        }
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    private CodexMessageResult parseResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode message = root.path("choices").path(0).path("message");
            String text = message.path("content").asText("");
            JsonNode usage = root.path("usage");
            Integer inputTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
            Integer outputTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            String model = root.path("model").asText(defaultModel);
            return new CodexMessageResult(true, text, model, inputTokens, outputTokens, null);
        } catch (Exception ex) {
            log.error("AI API 응답 파싱 실패", ex);
            return CodexMessageResult.error("AI API 응답 파싱 실패");
        }
    }

    public record CodexMessageResult(
        boolean success,
        String text,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        String errorMessage
    ) {
        public static CodexMessageResult disabled() {
            return new CodexMessageResult(false, null, null, null, null, "API 키 미설정");
        }

        public static CodexMessageResult error(String msg) {
            return new CodexMessageResult(false, null, null, null, null, msg);
        }
    }
}
