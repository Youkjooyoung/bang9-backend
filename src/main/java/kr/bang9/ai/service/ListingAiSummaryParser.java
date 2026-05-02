package kr.bang9.ai.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ListingAiSummaryParser {

    public ParsedSummary parse(String text) {
        if (text == null) {
            return new ParsedSummary("", List.of(), List.of());
        }
        String summary = extractSection(text, "[요약]", "[강점]");
        List<String> highlights = extractBullets(extractSection(text, "[강점]", "[주의사항]"));
        List<String> cautions = extractBullets(extractSection(text, "[주의사항]", null));
        return new ParsedSummary(summary.trim(), highlights, cautions);
    }

    public String joinLines(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return String.join("\n", items);
    }

    public List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .toList();
    }

    private String extractSection(String text, String start, String end) {
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return "";
        }
        startIndex += start.length();
        int endIndex = end == null ? text.length() : text.indexOf(end, startIndex);
        if (endIndex < 0) {
            endIndex = text.length();
        }
        return text.substring(startIndex, endIndex).trim();
    }

    private List<String> extractBullets(String section) {
        if (section == null || section.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String raw : section.split("\n")) {
            String line = normalizeBullet(raw.trim());
            if (!line.isBlank()) {
                result.add(line);
            }
        }
        return result;
    }

    private String normalizeBullet(String line) {
        if (line.startsWith("- ")) {
            return line.substring(2).trim();
        }
        if (line.startsWith("• ")) {
            return line.substring(2).trim();
        }
        if (line.matches("^\\d+\\.\\s+.*")) {
            return line.replaceFirst("^\\d+\\.\\s+", "").trim();
        }
        return line;
    }

    public record ParsedSummary(String summary, List<String> highlights, List<String> cautions) {}
}
