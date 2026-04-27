package kr.bang9.admin.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLog {
    private Long logId;
    private Long adminUserId;
    private String action;
    private String targetType;
    private Long targetId;
    private String payload;
    private LocalDateTime createdAt;
}
