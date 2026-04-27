package kr.bang9.external.portone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClient {

    private static final String BASE_URL = "https://api.iamport.kr";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final ObjectMapper objectMapper;

    @Value("${portone.imp-key:}")
    private String impKey;

    @Value("${portone.imp-secret:}")
    private String impSecret;

    public String issueAccessToken() {
        ensureCredentials();
        Map<String, String> body = Map.of(
            "imp_key", impKey,
            "imp_secret", impSecret
        );
        JsonNode response = webClient().post()
            .uri("/users/getToken")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(TIMEOUT)
            .block();
        return extractAccessToken(response);
    }

    public PortOneCertification fetchCertification(String impUid) {
        String accessToken = issueAccessToken();
        JsonNode response = webClient().get()
            .uri("/certifications/{impUid}", impUid)
            .header("Authorization", accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(TIMEOUT)
            .block();
        return mapCertification(response);
    }

    public PortOnePayment fetchPayment(String impUid) {
        String accessToken = issueAccessToken();
        JsonNode response = webClient().get()
            .uri("/payments/{impUid}", impUid)
            .header("Authorization", accessToken)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(TIMEOUT)
            .block();
        return mapPayment(response);
    }

    public PortOneCancellation cancelPayment(String impUid, Integer amount, String reason) {
        if (impUid == null || impUid.isBlank()) {
            throw new CustomException(ErrorCode.ERR_INVALID_PARAMETER, "결제 식별자가 필요합니다.");
        }
        String accessToken = issueAccessToken();
        Map<String, Object> body = new HashMap<>();
        body.put("imp_uid", impUid);
        if (amount != null && amount > 0) {
            body.put("amount", amount);
        }
        if (reason != null && !reason.isBlank()) {
            body.put("reason", reason);
        }
        JsonNode response = webClient().post()
            .uri("/payments/cancel")
            .header("Authorization", accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .timeout(TIMEOUT)
            .onErrorResume(ex -> {
                log.error("PortOne 결제 취소 호출 실패", ex);
                throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "PG 결제 취소 요청에 실패했습니다.");
            })
            .block();
        return mapCancellation(response);
    }

    private void ensureCredentials() {
        if (impKey == null || impKey.isBlank() || impSecret == null || impSecret.isBlank()) {
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 연동 설정이 누락되었습니다.");
        }
    }

    private WebClient webClient() {
        return WebClient.builder().baseUrl(BASE_URL).build();
    }

    private String extractAccessToken(JsonNode response) {
        if (response == null || !response.hasNonNull("response")) {
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 토큰을 발급받지 못했습니다.");
        }
        JsonNode data = response.get("response");
        if (!data.hasNonNull("access_token")) {
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 토큰 응답이 올바르지 않습니다.");
        }
        return data.get("access_token").asText();
    }

    private PortOnePayment mapPayment(JsonNode response) {
        if (response == null || !response.hasNonNull("response")) {
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 정보를 조회하지 못했습니다.");
        }
        JsonNode data = response.get("response");
        try {
            String raw = objectMapper.writeValueAsString(data);
            return new PortOnePayment(
                text(data, "imp_uid"),
                text(data, "merchant_uid"),
                text(data, "status"),
                text(data, "pay_method"),
                data.hasNonNull("amount") ? data.get("amount").asInt() : 0,
                data.hasNonNull("paid_at") && data.get("paid_at").asLong() > 0
                    ? data.get("paid_at").asLong() : 0L,
                raw
            );
        } catch (Exception e) {
            log.error("PortOne 응답 파싱 실패", e);
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "결제 응답 처리에 실패했습니다.");
        }
    }

    private PortOneCancellation mapCancellation(JsonNode response) {
        if (response == null || !response.hasNonNull("response")) {
            int code = response != null && response.hasNonNull("code") ? response.get("code").asInt(0) : -1;
            String message = response != null && response.hasNonNull("message") ? response.get("message").asText() : null;
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED,
                message != null ? message : "PG 결제 취소 응답이 올바르지 않습니다. (code=" + code + ")");
        }
        JsonNode data = response.get("response");
        try {
            String raw = objectMapper.writeValueAsString(data);
            int cancelAmount = 0;
            if (data.hasNonNull("cancel_amount")) {
                cancelAmount = data.get("cancel_amount").asInt();
            } else if (data.hasNonNull("amount")) {
                cancelAmount = data.get("amount").asInt();
            }
            long cancelledAt = data.hasNonNull("cancelled_at") ? data.get("cancelled_at").asLong() : 0L;
            return new PortOneCancellation(
                text(data, "imp_uid"),
                text(data, "merchant_uid"),
                text(data, "status"),
                cancelAmount,
                cancelledAt,
                text(data, "pg_tid"),
                raw
            );
        } catch (Exception e) {
            log.error("PortOne 취소 응답 파싱 실패", e);
            throw new CustomException(ErrorCode.ERR_PAYMENT_FAILED, "PG 취소 응답 처리에 실패했습니다.");
        }
    }

    private PortOneCertification mapCertification(JsonNode response) {
        if (response == null || !response.hasNonNull("response")) {
            throw new CustomException(ErrorCode.ERR_IDENTITY_VERIFICATION_FAILED, "본인인증 정보를 조회하지 못했습니다.");
        }
        JsonNode data = response.get("response");
        boolean certified = data.hasNonNull("certified") && data.get("certified").asBoolean();
        if (!certified) {
            throw new CustomException(ErrorCode.ERR_IDENTITY_VERIFICATION_FAILED, "본인인증이 완료되지 않았습니다.");
        }
        String name = text(data, "name");
        String birth = text(data, "birth");
        String gender = text(data, "gender");
        String uniqueKey = text(data, "unique_key");
        long certifiedAt = data.hasNonNull("certified_at") ? data.get("certified_at").asLong() : 0L;
        String rrn = buildRrn(birth, gender);
        return new PortOneCertification(
            text(data, "imp_uid"),
            name,
            rrn,
            uniqueKey,
            certifiedAt
        );
    }

    private String buildRrn(String birth, String gender) {
        if (birth == null || birth.length() != 10) return null;
        String yy = birth.substring(2, 4);
        String mm = birth.substring(5, 7);
        String dd = birth.substring(8, 10);
        String gNum = switch (gender == null ? "" : gender) {
            case "male" -> yearPrefix(birth.substring(0, 4), true);
            case "female" -> yearPrefix(birth.substring(0, 4), false);
            default -> "0";
        };
        return yy + mm + dd + gNum;
    }

    private String yearPrefix(String year, boolean male) {
        int y = Integer.parseInt(year);
        if (y < 2000) {
            return male ? "1" : "2";
        }
        return male ? "3" : "4";
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
