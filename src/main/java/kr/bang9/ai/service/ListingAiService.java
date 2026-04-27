package kr.bang9.ai.service;

import kr.bang9.ai.client.AnthropicClient;
import kr.bang9.ai.dao.ListingAiSummaryDao;
import kr.bang9.ai.domain.ListingAiSummary;
import kr.bang9.ai.dto.AiSummaryResponse;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import kr.bang9.listing.dao.ListingDao;
import kr.bang9.listing.dto.ListingDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingAiService {

    private static final String PROMPT_VERSION = "v1";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DISABLED = "DISABLED";

    private static final String SYSTEM_PROMPT =
            "당신은 한국의 원룸/오피스텔/빌라 매물을 간결하고 정확하게 요약하는 부동산 어시스턴트입니다. " +
            "답변은 반드시 한국어로 작성하며, 과장 없이 팩트 기반으로 설명합니다.";

    private final AnthropicClient anthropicClient;
    private final ListingAiSummaryDao aiSummaryDao;
    private final ListingDao listingDao;

    @Transactional(readOnly = true)
    public AiSummaryResponse getCachedSummary(Long listingId) {
        ListingAiSummary cached = aiSummaryDao.findByListingId(listingId);
        if (cached == null) {
            throw new CustomException(ErrorCode.ERR_NOT_FOUND, "AI 요약이 아직 생성되지 않았습니다.");
        }
        return toResponse(cached);
    }

    @Transactional
    public AiSummaryResponse generate(Long listingId) {
        ListingDetail detail = listingDao.findDetail(listingId)
                .orElseThrow(() -> new CustomException(ErrorCode.ERR_NOT_FOUND, "매물을 찾을 수 없습니다."));
        List<String> optionCodes = listingDao.findOptionCodes(listingId);

        String inputHash = hashInput(detail, optionCodes);

        ListingAiSummary existing = aiSummaryDao.findByListingId(listingId);
        if (existing != null && STATUS_SUCCESS.equals(existing.getStatus())
                && inputHash.equals(existing.getInputHash())) {
            log.info("AI 요약 캐시 히트 (inputHash 일치) listingId={}", listingId);
            return toResponse(existing);
        }

        String userPrompt = buildPrompt(detail, optionCodes);
        AnthropicClient.AnthropicMessageResult result = anthropicClient.sendMessage(SYSTEM_PROMPT, userPrompt);

        ListingAiSummary summary;
        if (!result.success()) {
            String status = result.errorMessage() != null && result.errorMessage().contains("API 키 미설정")
                    ? STATUS_DISABLED : STATUS_FAILED;
            summary = ListingAiSummary.builder()
                    .listingId(listingId)
                    .model(anthropicClient.getDefaultModel())
                    .promptVersion(PROMPT_VERSION)
                    .summaryText("")
                    .inputHash(inputHash)
                    .status(status)
                    .errorMessage(truncate(result.errorMessage(), 500))
                    .build();
        } else {
            ParsedSummary parsed = parseSummary(result.text());
            summary = ListingAiSummary.builder()
                    .listingId(listingId)
                    .model(result.model())
                    .promptVersion(PROMPT_VERSION)
                    .summaryText(parsed.summary)
                    .highlights(joinLines(parsed.highlights))
                    .cautions(joinLines(parsed.cautions))
                    .inputHash(inputHash)
                    .inputTokens(result.inputTokens())
                    .outputTokens(result.outputTokens())
                    .status(STATUS_SUCCESS)
                    .build();
        }
        aiSummaryDao.upsert(summary);
        return toResponse(aiSummaryDao.findByListingId(listingId));
    }

    @Transactional
    public int runBatch(int limit) {
        List<Long> targetIds = aiSummaryDao.findListingIdsWithoutSummary(limit);
        int count = 0;
        for (Long listingId : targetIds) {
            try {
                generate(listingId);
                count++;
            } catch (Exception ex) {
                log.error("AI 요약 배치 실패 listingId={}", listingId, ex);
            }
        }
        log.info("AI 요약 배치 완료: {}/{} 건", count, targetIds.size());
        return count;
    }

    private String buildPrompt(ListingDetail detail, List<String> optionCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 매물 정보를 바탕으로 '2-3문장 핵심 요약', '강점 3가지', '유의사항 2가지'를 작성해주세요.\n");
        sb.append("반드시 아래 형식을 지켜주세요:\n\n");
        sb.append("[요약]\n(2-3문장)\n\n");
        sb.append("[강점]\n- 항목1\n- 항목2\n- 항목3\n\n");
        sb.append("[유의사항]\n- 항목1\n- 항목2\n\n");
        sb.append("--- 매물 정보 ---\n");
        sb.append("제목: ").append(nullToEmpty(detail.title())).append('\n');
        sb.append("룸타입: ").append(nullToEmpty(detail.roomType())).append('\n');
        sb.append("거래유형: ").append(nullToEmpty(detail.dealType())).append('\n');
        sb.append("보증금: ").append(formatMoney(detail.deposit())).append('\n');
        sb.append("월세: ").append(formatMoney(detail.monthlyRent())).append('\n');
        sb.append("관리비: ").append(formatMoney(detail.maintenanceFee())).append('\n');
        sb.append("전용면적: ").append(detail.areaM2()).append("m²\n");
        if (detail.floor() != null) {
            sb.append("층: ").append(detail.floor());
            if (detail.totalFloor() != null) {
                sb.append("/").append(detail.totalFloor());
            }
            sb.append('\n');
        }
        sb.append("주소: ").append(nullToEmpty(detail.addressRoad())).append('\n');
        if (optionCodes != null && !optionCodes.isEmpty()) {
            sb.append("옵션: ").append(String.join(", ", optionCodes)).append('\n');
        }
        if (detail.description() != null && !detail.description().isBlank()) {
            sb.append("설명: ").append(detail.description().trim()).append('\n');
        }
        return sb.toString();
    }

    private ParsedSummary parseSummary(String text) {
        if (text == null) {
            return new ParsedSummary("", List.of(), List.of());
        }
        String summary = extractSection(text, "[요약]", "[강점]");
        List<String> highlights = extractBullets(extractSection(text, "[강점]", "[유의사항]"));
        List<String> cautions = extractBullets(extractSection(text, "[유의사항]", null));
        return new ParsedSummary(summary.trim(), highlights, cautions);
    }

    private String extractSection(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) return "";
        s += start.length();
        int e = end == null ? text.length() : text.indexOf(end, s);
        if (e < 0) e = text.length();
        return text.substring(s, e).trim();
    }

    private List<String> extractBullets(String section) {
        if (section == null || section.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String raw : section.split("\n")) {
            String line = raw.trim();
            if (line.startsWith("- ")) line = line.substring(2).trim();
            else if (line.startsWith("•")) line = line.substring(1).trim();
            else if (line.matches("^\\d+\\.\\s+.*")) line = line.replaceFirst("^\\d+\\.\\s+", "").trim();
            if (!line.isBlank()) result.add(line);
        }
        return result;
    }

    private String joinLines(List<String> items) {
        if (items == null || items.isEmpty()) return null;
        return String.join("\n", items);
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private AiSummaryResponse toResponse(ListingAiSummary summary) {
        return new AiSummaryResponse(
                summary.getListingId(),
                summary.getModel(),
                summary.getSummaryText(),
                splitLines(summary.getHighlights()),
                splitLines(summary.getCautions()),
                summary.getStatus(),
                summary.getUpdatedAt()
        );
    }

    private String hashInput(ListingDetail detail, List<String> optionCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append(detail.title()).append('|');
        sb.append(detail.description()).append('|');
        sb.append(detail.roomType()).append('|');
        sb.append(detail.dealType()).append('|');
        sb.append(detail.deposit()).append('|');
        sb.append(detail.monthlyRent()).append('|');
        sb.append(detail.maintenanceFee()).append('|');
        sb.append(detail.areaM2()).append('|');
        sb.append(detail.floor()).append('|');
        sb.append(detail.totalFloor()).append('|');
        sb.append(detail.addressRoad()).append('|');
        if (optionCodes != null) {
            sb.append(String.join(",", optionCodes));
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception ex) {
            return String.valueOf(sb.toString().hashCode());
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String formatMoney(Integer value) {
        if (value == null || value == 0) return "0";
        return String.format("%,d 만원", value);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record ParsedSummary(String summary, List<String> highlights, List<String> cautions) {}
}
