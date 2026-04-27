package kr.bang9.batch;

import kr.bang9.external.publicdata.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicDataScheduler {

    private final PublicDataService publicDataService;

    @Scheduled(cron = "0 30 3 * * *", zone = "Asia/Seoul")
    public void dailyCrawl() {
        try {
            int total = publicDataService.crawlDefaults();
            log.info("[스케줄러] 공공데이터 일일 크롤 완료. 총 {}건", total);
        } catch (Exception e) {
            log.error("[스케줄러] 공공데이터 일일 크롤 실패: {}", e.getMessage(), e);
        }
    }
}
