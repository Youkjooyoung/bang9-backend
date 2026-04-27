package kr.bang9.address.service;

import kr.bang9.address.dto.GeocodingResult;
import kr.bang9.address.dto.ReverseGeocodingResult;
import kr.bang9.external.geocoding.GeocodingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final GeocodingProvider geocodingProvider;

    public List<GeocodingResult> search(String query) {
        return geocodingProvider.searchAddress(query);
    }

    public GeocodingResult findFirst(String query) {
        List<GeocodingResult> results = geocodingProvider.searchAddress(query);
        return results.isEmpty() ? null : results.get(0);
    }

    public ReverseGeocodingResult reverse(double latitude, double longitude) {
        return geocodingProvider.reverseGeocode(latitude, longitude);
    }

    public String activeProvider() {
        return geocodingProvider.providerName();
    }
}
