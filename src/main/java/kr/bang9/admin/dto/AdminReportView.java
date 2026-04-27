package kr.bang9.admin.dto;

import java.time.LocalDateTime;

public record AdminReportView(
    Long reportId,
    Long listingId,
    String listingTitle,
    Long reporterUserId,
    String reporterNickname,
    String reason,
    String status,
    LocalDateTime createdAt
) {
}
