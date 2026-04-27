package kr.bang9.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiSummaryResponse(
        Long listingId,
        String model,
        String summaryText,
        List<String> highlights,
        List<String> cautions,
        String status,
        LocalDateTime updatedAt
) {}
