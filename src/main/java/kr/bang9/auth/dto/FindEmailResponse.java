package kr.bang9.auth.dto;

import java.time.LocalDateTime;

public record FindEmailResponse(
    String maskedEmail,
    LocalDateTime createdAt
) {
}
