package site.dataon.hyeyum.search;

import java.util.Locale;

final class KoreanSearchText {

    private static final char[] CHOSUNG = {
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char[] JUNGSUNG = {
        'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
        'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
    };
    private static final char[] JONGSUNG = {
        '\0', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
        'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
        'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    private KoreanSearchText() {}

    static String normalizedText(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            builder.append(value);
        }
        return builder.toString().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    static String chosung(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> appendChosung(builder, codePoint));
        return builder.toString();
    }

    static String jamo(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() * 3);
        value.codePoints().forEach(codePoint -> appendJamo(builder, codePoint));
        return builder.toString();
    }

    private static void appendChosung(StringBuilder builder, int codePoint) {
        int syllableIndex = codePoint - 0xAC00;
        if (syllableIndex >= 0 && syllableIndex <= 11171) {
            builder.append(CHOSUNG[syllableIndex / (21 * 28)]);
            return;
        }
        builder.appendCodePoint(codePoint);
    }

    private static void appendJamo(StringBuilder builder, int codePoint) {
        int syllableIndex = codePoint - 0xAC00;
        if (syllableIndex < 0 || syllableIndex > 11171) {
            builder.appendCodePoint(codePoint);
            return;
        }
        int chosungIndex = syllableIndex / (21 * 28);
        int jungsungIndex = (syllableIndex % (21 * 28)) / 28;
        int jongsungIndex = syllableIndex % 28;
        builder.append(CHOSUNG[chosungIndex]);
        builder.append(JUNGSUNG[jungsungIndex]);
        if (jongsungIndex > 0) {
            builder.append(JONGSUNG[jongsungIndex]);
        }
    }
}
