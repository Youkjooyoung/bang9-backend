package kr.bang9.common.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class ProfanityFilter {

    private static final List<String> BAD_WORDS = List.of(
        "개새끼", "개놈", "개년", "개좆", "개씹", "개지랄",
        "씨발", "씨팔", "시발", "시팔", "ㅅㅂ",
        "병신", "ㅂㅅ",
        "지랄", "ㅈㄹ",
        "꺼져", "꺼지",
        "미친놈", "미친년", "미친새끼", "미친", "ㅁㅊ",
        "fuck", "bitch", "shit", "asshole", "bastard",
        "fucked", "fucking", "fck", "f*ck",
        "씨바", "씨바라", "씨바라기",
        "좆", "보지", "자지",
        "새끼", "놈", "년", "놈팡이",
        "죽어", "죽어라", "뒤져", "뒤져라",
        "창녀", "창년", "잡년",
        "쓰레기", "찐따", "병신같은"
    );

    private static final List<Pattern> PATTERNS = BAD_WORDS.stream()
        .map(word -> Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE))
        .toList();

    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        String normalized = text.replace(" ", "");
        for (Pattern pattern : PATTERNS) {
            if (pattern.matcher(normalized).find()) return true;
        }
        return false;
    }

    public String getMatchedWord(String text) {
        if (text == null || text.isBlank()) return null;
        String normalized = text.replace(" ", "");
        for (int i = 0; i < PATTERNS.size(); i++) {
            if (PATTERNS.get(i).matcher(normalized).find()) {
                return BAD_WORDS.get(i);
            }
        }
        return null;
    }
}
