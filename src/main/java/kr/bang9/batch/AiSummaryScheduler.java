package kr.bang9.batch;

import kr.bang9.ai.service.ListingAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSummaryScheduler {

    private static final int BATCH_LIMIT = 20;

    private final ListingAiService listingAiService;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void dailyAiSummary() {
        try {
            int count = listingAiService.runBatch(BATCH_LIMIT);
            log.info("[스케줄러] AI 요약 배치 완료. {}건 생성", count);
        } catch (Exception ex) {
            log.error("[스케줄러] AI 요약 배치 실패", ex);
        }
    }
}
