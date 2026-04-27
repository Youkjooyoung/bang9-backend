package kr.bang9.external.publicdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class PublicDataClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1613000/RTMSDataSvcRHRent";
    private static final String PATH_RENT = "/getRTMSDataSvcRHRent";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String serviceKey;

    public PublicDataClient(WebClient.Builder builder,
                            ObjectMapper objectMapper,
                            @Value("${publicdata.service-key:}") String serviceKey) {
        this.webClient = builder.baseUrl(BASE_URL).build();
        this.objectMapper = objectMapper;
        this.serviceKey = serviceKey;
    }

    public List<PublicRealEstate> fetchRentals(String regionCode, String yearMonth) {
        if (!StringUtils.hasText(serviceKey)) {
            log.warn("publicdata.service-key 가 비어있어 공공데이터 API 호출을 건너뜁니다.");
            return List.of();
        }
        String response = webClient.get()
            .uri(uriBuilder -> uriBuilder.path(PATH_RENT)
                .queryParam("serviceKey", serviceKey)
                .queryParam("LAWD_CD", regionCode)
                .queryParam("DEAL_YMD", yearMonth)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 500)
                .queryParam("_type", "json")
                .build())
            .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
            .acceptCharset(StandardCharsets.UTF_8)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> {
                log.error("공공데이터 API 호출 실패: {}", e.getMessage());
                return Mono.empty();
            })
            .block();
        if (!StringUtils.hasText(response)) return List.of();
        return parseRentals(response, regionCode, yearMonth);
    }

    private List<PublicRealEstate> parseRentals(String body, String regionCode, String yearMonth) {
        List<PublicRealEstate> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.at("/response/body/items/item");
            if (items.isMissingNode()) return list;
            Iterator<JsonNode> iterator = items.isArray() ? items.elements() : List.of(items).iterator();
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                PublicRealEstate dto = mapNode(node, regionCode, yearMonth);
                if (dto != null) list.add(dto);
            }
        } catch (Exception e) {
            log.warn("공공데이터 응답 파싱 실패: {}", e.getMessage());
        }
        return list;
    }

    private PublicRealEstate mapNode(JsonNode node, String regionCode, String yearMonth) {
        try {
            String deposit = text(node, "deposit").replace(",", "").trim();
            String monthly = text(node, "monthlyRent").replace(",", "").trim();
            String area = text(node, "excluUseAr").trim();
            int depositValue = deposit.isEmpty() ? 0 : Integer.parseInt(deposit);
            int monthlyValue = monthly.isEmpty() ? 0 : Integer.parseInt(monthly);
            BigDecimal areaValue = area.isEmpty()
                ? BigDecimal.ZERO
                : new BigDecimal(area).setScale(2, RoundingMode.HALF_UP);
            String dealType = monthlyValue > 0 ? "MONTHLY" : "JEONSE";
            String floorText = text(node, "floor").trim();
            Short floor = floorText.isEmpty() ? null : Short.parseShort(floorText);
            return PublicRealEstate.builder()
                .regionCode(regionCode)
                .dealType(dealType)
                .areaM2(areaValue)
                .deposit(depositValue)
                .monthlyRent(monthlyValue)
                .dealYearMonth(yearMonth)
                .buildingName(text(node, "mhouseNm"))
                .floor(floor)
                .build();
        } catch (Exception e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }
}
