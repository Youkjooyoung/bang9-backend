package kr.bang9.order.refund.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Refund {

    private Long refundId;
    private Long orderId;
    private Long userId;
    private Integer amount;
    private String reason;
    private String status;
    private Long adminUserId;
    private String adminNote;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private String pgCancellationId;
    private LocalDateTime processedAt;
    private LocalDateTime updatedAt;
}
