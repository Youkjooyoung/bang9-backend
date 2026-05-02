package kr.bang9.ai.service;

import kr.bang9.ai.client.CodexClient;
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

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingAiService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DISABLED = "DISABLED";

    private final CodexClient codexClient;
    private final ListingAiSummaryDao aiSummaryDao;
    private final ListingDao listingDao;
    private final ListingAiPromptFactory promptFactory;
    private final ListingAiSummaryParser summaryParser;

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
        String inputHash = promptFactory.hashInput(detail, optionCodes);

        ListingAiSummary existing = aiSummaryDao.findByListingId(listingId);
        if (isReusable(existing, inputHash)) {
            log.info("AI 요약 캐시 히트 listingId={}", listingId);
            return toResponse(existing);
        }

        CodexClient.CodexMessageResult result = codexClient.sendMessage(
            promptFactory.systemPrompt(),
            promptFactory.buildPrompt(detail, optionCodes)
        );

        ListingAiSummary summary = result.success()
            ? successSummary(listingId, inputHash, result)
            : failedSummary(listingId, inputHash, result);
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
        log.info("AI 요약 배치 완료: {}/{}건", count, targetIds.size());
        return count;
    }

    private boolean isReusable(ListingAiSummary existing, String inputHash) {
        return existing != null
            && STATUS_SUCCESS.equals(existing.getStatus())
            && inputHash.equals(existing.getInputHash());
    }

    private ListingAiSummary successSummary(Long listingId, String inputHash, CodexClient.CodexMessageResult result) {
        ListingAiSummaryParser.ParsedSummary parsed = summaryParser.parse(result.text());
        return ListingAiSummary.builder()
            .listingId(listingId)
            .model(result.model())
            .promptVersion(promptFactory.promptVersion())
            .summaryText(parsed.summary())
            .highlights(summaryParser.joinLines(parsed.highlights()))
            .cautions(summaryParser.joinLines(parsed.cautions()))
            .inputHash(inputHash)
            .inputTokens(result.inputTokens())
            .outputTokens(result.outputTokens())
            .status(STATUS_SUCCESS)
            .build();
    }

    private ListingAiSummary failedSummary(Long listingId, String inputHash, CodexClient.CodexMessageResult result) {
        String errorMessage = result.errorMessage();
        return ListingAiSummary.builder()
            .listingId(listingId)
            .model(codexClient.getDefaultModel())
            .promptVersion(promptFactory.promptVersion())
            .summaryText("")
            .inputHash(inputHash)
            .status(isDisabled(errorMessage) ? STATUS_DISABLED : STATUS_FAILED)
            .errorMessage(truncate(errorMessage, 500))
            .build();
    }

    private AiSummaryResponse toResponse(ListingAiSummary summary) {
        return new AiSummaryResponse(
            summary.getListingId(),
            summary.getModel(),
            summary.getSummaryText(),
            summaryParser.splitLines(summary.getHighlights()),
            summaryParser.splitLines(summary.getCautions()),
            summary.getStatus(),
            summary.getUpdatedAt()
        );
    }

    private boolean isDisabled(String errorMessage) {
        return errorMessage != null
            && (errorMessage.contains("API 키") || errorMessage.toLowerCase().contains("api key"));
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
