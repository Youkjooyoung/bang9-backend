package kr.bang9.external.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import kr.bang9.address.dto.GeocodingResult;
import kr.bang9.address.dto.ReverseGeocodingResult;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "vworld", matchIfMissing = true)
public class VWorldGeocodingProvider implements GeocodingProvider {

    private static final String VWORLD_BASE_URL = "https://api.vworld.kr";
    private static final String GETCOORD_PATH = "/req/address";

    private final RestClient restClient;
    private final String apiKey;

    public VWorldGeocodingProvider(@Value("${app.vworld.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
            .baseUrl(VWORLD_BASE_URL)
            .build();
    }

    @Override
    public String providerName() {
        return "VWORLD";
    }

    @Override
    public List<GeocodingResult> searchAddress(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld API 키가 설정되지 않았습니다.");
        }
        if (query == null || query.isBlank()) {
            return List.of();
        }

        GeocodingResult roadHit = callApi(query, "road");
        if (roadHit != null) return List.of(roadHit);

        GeocodingResult parcelHit = callApi(query, "parcel");
        if (parcelHit != null) return List.of(parcelHit);

        return List.of();
    }

    @Override
    public ReverseGeocodingResult reverseGeocode(double latitude, double longitude) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld API 키가 설정되지 않았습니다.");
        }

        String point = longitude + "," + latitude;
        String uri = UriComponentsBuilder.fromPath(GETCOORD_PATH)
            .queryParam("service", "address")
            .queryParam("request", "getaddress")
            .queryParam("version", "2.0")
            .queryParam("crs", "epsg:4326")
            .queryParam("point", point)
            .queryParam("format", "json")
            .queryParam("type", "both")
            .queryParam("zipcode", "false")
            .queryParam("simple", "false")
            .queryParam("key", apiKey)
            .build()
            .toUriString();

        try {
            JsonNode root = restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode.class);

            if (root == null) return null;
            JsonNode response = root.get("response");
            if (response == null) return null;

            String status = textOrNull(response, "status");
            if (!"OK".equals(status)) {
                if ("ERROR".equals(status)) {
                    JsonNode err = response.get("error");
                    String code = err != null ? textOrNull(err, "code") : null;
                    String text = err != null ? textOrNull(err, "text") : null;
                    log.error("VWorld reverse ERROR code={} text={}", code, text);
                    if ("INVALID_KEY".equals(code) || "UNAUTHENTICATED".equals(code)) {
                        throw new CustomException(ErrorCode.ERR_INTERNAL,
                            "VWorld API 인증 실패: 키를 확인해주세요.");
                    }
                }
                return null;
            }

            JsonNode result = response.get("result");
            if (result == null || !result.isArray() || result.isEmpty()) return null;

            JsonNode picked = result.get(0);
            String text = textOrNull(picked, "text");
            JsonNode structure = picked.get("structure");
            String depth1 = structure != null ? textOrNull(structure, "level1") : null;
            String depth2 = structure != null ? textOrNull(structure, "level2") : null;
            String depth3 = null;
            if (structure != null) {
                String level4L = textOrNull(structure, "level4L");
                String level4A = textOrNull(structure, "level4A");
                String level5 = textOrNull(structure, "level5");
                if (level4L != null && !level4L.isBlank()) depth3 = level4L;
                else if (level4A != null && !level4A.isBlank()) depth3 = level4A;
                else if (level5 != null && !level5.isBlank()) depth3 = level5;
            }

            return new ReverseGeocodingResult(
                text,
                depth1,
                depth2,
                depth3,
                latitude,
                longitude
            );
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("VWorld reverse 401 Unauthorized body={}", ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld API 인증 실패: 키가 올바른지 확인해주세요.");
        } catch (HttpClientErrorException ex) {
            log.error("VWorld reverse {} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld API 호출 오류: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("VWorld reverse 네트워크 오류: {}", ex.getMessage());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld 서버 연결에 실패했습니다.");
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("VWorld 좌표→주소 실패 lat={} lng={}", latitude, longitude, ex);
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "주소 변환 서비스 오류: " + ex.getClass().getSimpleName());
        }
    }

    private GeocodingResult callApi(String query, String type) {
        String uri = UriComponentsBuilder.fromPath(GETCOORD_PATH)
            .queryParam("service", "address")
            .queryParam("request", "getcoord")
            .queryParam("version", "2.0")
            .queryParam("crs", "epsg:4326")
            .queryParam("format", "json")
            .queryParam("type", type)
            .queryParam("refine", "true")
            .queryParam("simple", "false")
            .queryParam("address", query)
            .queryParam("key", apiKey)
            .build()
            .toUriString();

        try {
            JsonNode root = restClient.get()
                .uri(uri)
                .retrieve()
                .body(JsonNode.class);

            if (root == null) return null;
            JsonNode response = root.get("response");
            if (response == null) return null;

            String status = textOrNull(response, "status");
            if (!"OK".equals(status)) {
                if ("ERROR".equals(status)) {
                    JsonNode err = response.get("error");
                    String code = err != null ? textOrNull(err, "code") : null;
                    String text = err != null ? textOrNull(err, "text") : null;
                    log.error("VWorld API ERROR type={} code={} text={}", type, code, text);
                    if ("INVALID_KEY".equals(code) || "UNAUTHENTICATED".equals(code)) {
                        throw new CustomException(ErrorCode.ERR_INTERNAL,
                            "VWorld API 인증 실패: 키를 확인해주세요.");
                    }
                }
                return null;
            }

            JsonNode result = response.get("result");
            if (result == null) return null;
            JsonNode point = result.get("point");
            if (point == null) return null;

            Double longitude = doubleOrNull(point, "x");
            Double latitude = doubleOrNull(point, "y");
            if (latitude == null || longitude == null) return null;

            String refinedAddress = null;
            JsonNode refined = response.get("refined");
            if (refined != null) {
                refinedAddress = textOrNull(refined, "text");
            }
            String resolved = refinedAddress != null ? refinedAddress : query;

            String roadAddress = "road".equals(type) ? resolved : null;
            String addressName = "parcel".equals(type) ? resolved : resolved;

            return new GeocodingResult(
                addressName,
                roadAddress != null ? roadAddress : addressName,
                latitude,
                longitude
            );
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("VWorld API 401 Unauthorized body={}", ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld API 인증 실패: 키가 올바른지 확인해주세요.");
        } catch (HttpClientErrorException ex) {
            log.error("VWorld API {} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld API 호출 오류: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("VWorld API 네트워크 오류: {}", ex.getMessage());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "VWorld 서버 연결에 실패했습니다.");
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("VWorld 주소 검색 실패 query={} type={}", query, type, ex);
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "주소 검색 서비스 오류: " + ex.getClass().getSimpleName());
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private Double doubleOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        try {
            return Double.parseDouble(v.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
