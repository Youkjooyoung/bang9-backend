package kr.bang9.address.controller;

import kr.bang9.address.dto.GeocodingResult;
import kr.bang9.address.dto.ReverseGeocodingResult;
import kr.bang9.address.service.GeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
public class GeocodingController {

    private final GeocodingService geocodingService;

    @GetMapping("/search")
    public ResponseEntity<List<GeocodingResult>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(geocodingService.search(query));
    }

    @GetMapping("/first")
    public ResponseEntity<GeocodingResult> findFirst(@RequestParam("query") String query) {
        GeocodingResult result = geocodingService.findFirst(query);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reverse")
    public ResponseEntity<ReverseGeocodingResult> reverse(
        @RequestParam("lat") double latitude,
        @RequestParam("lng") double longitude
    ) {
        ReverseGeocodingResult result = geocodingService.reverse(latitude, longitude);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }
}
