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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "kakao")
public class KakaoGeocodingProvider implements GeocodingProvider {

    private static final String KAKAO_LOCAL_BASE_URL = "https://dapi.kakao.com";
    private static final String SEARCH_ADDRESS_PATH = "/v2/local/search/address.json";
    private static final String COORD2REGION_PATH = "/v2/local/geo/coord2regioncode.json";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoGeocodingProvider(@Value("${app.kakao.rest-api-key:}") String restApiKey) {
        this.restApiKey = restApiKey;
        this.restClient = RestClient.builder()
            .baseUrl(KAKAO_LOCAL_BASE_URL)
            .build();
    }

    @Override
    public String providerName() {
        return "KAKAO";
    }

    @Override
    public List<GeocodingResult> searchAddress(String query) {
        GeocodingJson.requireApiKey(restApiKey, "Kakao REST API 키가 설정되지 않았습니다.");
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String uri = UriComponentsBuilder.fromPath(SEARCH_ADDRESS_PATH)
            .queryParam("query", query)
            .queryParam("size", 10)
            .build()
            .toUriString();

        try {
            JsonNode root = restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .body(JsonNode.class);

            if (root == null || !root.has("documents")) {
                return List.of();
            }

            List<GeocodingResult> results = new ArrayList<>();
            for (JsonNode doc : root.get("documents")) {
                GeocodingResult item = toResult(doc);
                if (item != null) results.add(item);
            }
            return results;
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("Kakao API 401 Unauthorized body={}", ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao API 인증 실패: REST API 키가 올바른지 확인해주세요.");
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Kakao API 403 Forbidden body={}", ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao 개발자 콘솔에서 '카카오맵' 제품을 활성화해주세요. (Local API 포함)");
        } catch (HttpClientErrorException ex) {
            log.error("Kakao API {} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao API 호출 오류: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("Kakao API 네트워크 오류: {}", ex.getMessage());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao 서버 연결에 실패했습니다.");
        } catch (Exception ex) {
            log.error("Kakao 주소 검색 실패 query={}", query, ex);
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "주소 검색 서비스 오류: " + ex.getClass().getSimpleName());
        }
    }

    @Override
    public ReverseGeocodingResult reverseGeocode(double latitude, double longitude) {
        GeocodingJson.requireApiKey(restApiKey, "Kakao REST API 키가 설정되지 않았습니다.");

        String uri = UriComponentsBuilder.fromPath(COORD2REGION_PATH)
            .queryParam("x", longitude)
            .queryParam("y", latitude)
            .build()
            .toUriString();

        try {
            JsonNode root = restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .body(JsonNode.class);

            if (root == null || !root.has("documents")) {
                return null;
            }

            JsonNode documents = root.get("documents");
            if (documents == null || !documents.isArray() || documents.isEmpty()) {
                return null;
            }

            JsonNode picked = null;
            for (JsonNode doc : documents) {
                String type = GeocodingJson.textOrNull(doc, "region_type");
                if ("H".equals(type)) {
                    picked = doc;
                    break;
                }
            }
            if (picked == null) picked = documents.get(0);

            String addressName = GeocodingJson.textOrNull(picked, "address_name");
            String depth1 = GeocodingJson.textOrNull(picked, "region_1depth_name");
            String depth2 = GeocodingJson.textOrNull(picked, "region_2depth_name");
            String depth3 = GeocodingJson.textOrNull(picked, "region_3depth_name");

            return new ReverseGeocodingResult(
                addressName,
                depth1,
                depth2,
                depth3,
                latitude,
                longitude
            );
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("Kakao reverse 401 Unauthorized body={}", ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao API 인증 실패: REST API 키가 올바른지 확인해주세요.");
        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Kakao reverse 403 Forbidden body={}", ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao 개발자 콘솔에서 '카카오맵' 제품을 활성화해주세요. (Local API 포함)");
        } catch (HttpClientErrorException ex) {
            log.error("Kakao reverse {} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao API 호출 오류: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            log.error("Kakao reverse 네트워크 오류: {}", ex.getMessage());
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "Kakao 서버 연결에 실패했습니다.");
        } catch (Exception ex) {
            log.error("Kakao 좌표→주소 실패 lat={} lng={}", latitude, longitude, ex);
            throw new CustomException(ErrorCode.ERR_INTERNAL,
                "주소 변환 서비스 오류: " + ex.getClass().getSimpleName());
        }
    }

    private GeocodingResult toResult(JsonNode doc) {
        if (doc == null) return null;
        String addressName = GeocodingJson.textOrNull(doc, "address_name");
        String roadAddressName = null;
        JsonNode roadAddress = doc.get("road_address");
        if (roadAddress != null && !roadAddress.isNull()) {
            roadAddressName = GeocodingJson.textOrNull(roadAddress, "address_name");
        }
        Double latitude = GeocodingJson.doubleOrNull(doc, "y");
        Double longitude = GeocodingJson.doubleOrNull(doc, "x");
        if (latitude == null || longitude == null) return null;
        return new GeocodingResult(
            addressName,
            roadAddressName != null ? roadAddressName : addressName,
            latitude,
            longitude
        );
    }

}
