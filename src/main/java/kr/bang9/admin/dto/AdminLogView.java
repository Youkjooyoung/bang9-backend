package kr.bang9.admin.dto;

import java.time.LocalDateTime;

public record AdminLogView(
    Long logId,
    Long adminUserId,
    String adminNickname,
    String action,
    String targetType,
    Long targetId,
    String payload,
    LocalDateTime createdAt
) {
}
