package kr.bang9.ai.service;

import kr.bang9.listing.dto.ListingDetail;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class ListingAiPromptFactory {

    static final String PROMPT_VERSION = "v1";
    private static final String SYSTEM_PROMPT =
        "당신은 한국 원룸, 오피스텔, 빌라 매물을 간결하고 정확하게 요약하는 부동산 어시스턴트입니다. " +
        "답변은 반드시 한국어로 작성하고, 과장 없이 입력된 사실 기반으로 설명합니다.";

    public String promptVersion() {
        return PROMPT_VERSION;
    }

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildPrompt(ListingDetail detail, List<String> optionCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("다음 매물 정보를 바탕으로 '2-3문장 핵심 요약', '강점 3가지', '주의사항 2가지'를 작성해주세요.\n");
        sb.append("반드시 아래 형식을 지켜주세요:\n\n");
        sb.append("[요약]\n(2-3문장)\n\n");
        sb.append("[강점]\n- 항목1\n- 항목2\n- 항목3\n\n");
        sb.append("[주의사항]\n- 항목1\n- 항목2\n\n");
        sb.append("--- 매물 정보 ---\n");
        sb.append("제목: ").append(nullToEmpty(detail.title())).append('\n');
        sb.append("룸타입: ").append(nullToEmpty(detail.roomType())).append('\n');
        sb.append("거래유형: ").append(nullToEmpty(detail.dealType())).append('\n');
        sb.append("보증금: ").append(formatMoney(detail.deposit())).append('\n');
        sb.append("월세: ").append(formatMoney(detail.monthlyRent())).append('\n');
        sb.append("관리비: ").append(formatMoney(detail.maintenanceFee())).append('\n');
        sb.append("전용면적: ").append(detail.areaM2()).append("m2\n");
        appendFloor(sb, detail);
        sb.append("주소: ").append(nullToEmpty(detail.addressRoad())).append('\n');
        if (optionCodes != null && !optionCodes.isEmpty()) {
            sb.append("옵션: ").append(String.join(", ", optionCodes)).append('\n');
        }
        if (detail.description() != null && !detail.description().isBlank()) {
            sb.append("설명: ").append(detail.description().trim()).append('\n');
        }
        return sb.toString();
    }

    public String hashInput(ListingDetail detail, List<String> optionCodes) {
        String raw = String.join("|",
            nullToEmpty(detail.title()),
            nullToEmpty(detail.description()),
            nullToEmpty(detail.roomType()),
            nullToEmpty(detail.dealType()),
            valueOf(detail.deposit()),
            valueOf(detail.monthlyRent()),
            valueOf(detail.maintenanceFee()),
            valueOf(detail.areaM2()),
            valueOf(detail.floor()),
            valueOf(detail.totalFloor()),
            nullToEmpty(detail.addressRoad()),
            optionCodes == null ? "" : String.join(",", optionCodes)
        );
        return sha256(raw);
    }

    private void appendFloor(StringBuilder sb, ListingDetail detail) {
        if (detail.floor() == null) {
            return;
        }
        sb.append("층: ").append(detail.floor());
        if (detail.totalFloor() != null) {
            sb.append('/').append(detail.totalFloor());
        }
        sb.append('\n');
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            return String.valueOf(raw.hashCode());
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatMoney(Integer value) {
        if (value == null || value == 0) {
            return "0";
        }
        return String.format("%,d 만원", value);
    }
}
