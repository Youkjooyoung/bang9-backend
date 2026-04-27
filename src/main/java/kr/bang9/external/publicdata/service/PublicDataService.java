package kr.bang9.external.publicdata.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kr.bang9.external.publicdata.PublicDataClient;
import kr.bang9.external.publicdata.PublicRealEstate;
import kr.bang9.external.publicdata.dao.PublicDataDao;
import kr.bang9.external.publicdata.dto.MarketPriceStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataService {

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final List<String> DEFAULT_REGIONS = List.of(
        "11680", "11650", "11710", "11440", "11470", "11170", "11110"
    );

    private final PublicDataClient publicDataClient;
    private final PublicDataDao publicDataDao;

    @Transactional(readOnly = true)
    public MarketPriceStats findStats(String regionCode, String dealType) {
        return publicDataDao.findStats(regionCode, dealType);
    }

    @Transactional(readOnly = true)
    public List<PublicRealEstate> findRecent(String regionCode, String dealType, int size) {
        return publicDataDao.findRecent(regionCode, dealType, size);
    }

    @Transactional
    public int crawlRegion(String regionCode, String yearMonth) {
        List<PublicRealEstate> rows = publicDataClient.fetchRentals(regionCode, yearMonth);
        if (rows.isEmpty()) {
            log.info("[공공데이터] {} {} 조회 결과 없음", regionCode, yearMonth);
            return 0;
        }
        publicDataDao.deleteByRegionAndMonth(regionCode, yearMonth);
        int inserted = publicDataDao.bulkInsert(rows);
        log.info("[공공데이터] {} {} {}건 저장", regionCode, yearMonth, inserted);
        return inserted;
    }

    @Transactional
    public int crawlDefaults() {
        String currentMonth = LocalDate.now().format(YEAR_MONTH_FMT);
        int total = 0;
        for (String region : DEFAULT_REGIONS) {
            total += crawlRegion(region, currentMonth);
        }
        return total;
    }
}
