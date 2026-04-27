package kr.bang9.external.geocoding;

import kr.bang9.address.dto.GeocodingResult;
import kr.bang9.address.dto.ReverseGeocodingResult;

import java.util.List;

public interface GeocodingProvider {

    List<GeocodingResult> searchAddress(String query);

    ReverseGeocodingResult reverseGeocode(double latitude, double longitude);

    String providerName();
}
