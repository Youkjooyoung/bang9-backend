package kr.bang9.ai.controller;

import kr.bang9.ai.dto.AiSummaryResponse;
import kr.bang9.ai.service.ListingAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ListingAiController {

    private final ListingAiService listingAiService;

    @GetMapping("/api/listings/{listingId}/ai-summary")
    public ResponseEntity<AiSummaryResponse> getSummary(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingAiService.getCachedSummary(listingId));
    }

    @PostMapping("/api/admin/listings/{listingId}/ai-summary/generate")
    public ResponseEntity<AiSummaryResponse> regenerate(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingAiService.generate(listingId));
    }
}
