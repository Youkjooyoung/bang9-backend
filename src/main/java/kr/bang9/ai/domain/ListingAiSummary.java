package kr.bang9.ai.domain;

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
public class ListingAiSummary {

    private Long summaryId;
    private Long listingId;
    private String model;
    private String promptVersion;
    private String summaryText;
    private String highlights;
    private String cautions;
    private String inputHash;
    private Integer inputTokens;
    private Integer outputTokens;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
