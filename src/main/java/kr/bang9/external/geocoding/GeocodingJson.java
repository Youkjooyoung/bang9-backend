package kr.bang9.external.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import kr.bang9.common.exception.CustomException;
import kr.bang9.common.exception.ErrorCode;

final class GeocodingJson {

    private GeocodingJson() {
    }

    static void requireApiKey(String apiKey, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new CustomException(ErrorCode.ERR_INTERNAL, message);
        }
    }

    static String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    static Double doubleOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
