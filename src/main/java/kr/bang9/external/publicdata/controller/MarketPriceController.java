package kr.bang9.external.publicdata.controller;

import java.util.List;
import kr.bang9.external.publicdata.PublicRealEstate;
import kr.bang9.external.publicdata.dto.MarketPriceStats;
import kr.bang9.external.publicdata.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketPriceController {

    private final PublicDataService publicDataService;

    @GetMapping("/stats")
    public ResponseEntity<MarketPriceStats> getStats(
        @RequestParam String regionCode,
        @RequestParam(defaultValue = "MONTHLY") String dealType
    ) {
        return ResponseEntity.ok(publicDataService.findStats(regionCode, dealType));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PublicRealEstate>> getRecent(
        @RequestParam String regionCode,
        @RequestParam(defaultValue = "MONTHLY") String dealType,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(publicDataService.findRecent(regionCode, dealType, size));
    }
}
