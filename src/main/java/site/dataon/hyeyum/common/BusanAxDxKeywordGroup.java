package site.dataon.hyeyum.common;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public enum BusanAxDxKeywordGroup {
    BTP_PROGRAM_KEYWORDS(
            "스마트공장",
            "스마트제조",
            "디지털 트윈",
            "디지털트윈",
            "제조데이터",
            "정보화솔루션",
            "자율형공장",
            "디지털협업공장",
            "AI 기반",
            "인공지능",
            "빅데이터",
            "블록체인",
            "ICT융합",
            "ICT 융합",
            "ICT＆헬스케어",
            "ICT&헬스케어",
            "비대면디지털",
            "디지털치의학",
            "디지털 상용화"),
    BTP_ITEM_KEYWORDS(
            "자동제어",
            "MES",
            "ERP",
            "SCM",
            "PLM",
            "IoT",
            "PLC",
            "AI",
            "인공지능",
            "스마트센서",
            "관제시스템",
            "통합관제",
            "실시간 관제",
            "디지털 트윈",
            "디지털트윈",
            "제조데이터",
            "데이터 활용",
            "데이터 분석",
            "APP 시제품",
            "앱 시제품",
            "플랫폼",
            "스마트 밴드",
            "ICT융복합",
            "ICT 융복합",
            "안전관리시스템",
            "의사결정시스템",
            "CCTV",
            "드론영상",
            "분석·예측",
            "분석 예측"),
    COMPANY_PROFILE_KEYWORDS(
            "시스템 소프트웨어 개발",
            "응용 소프트웨어 개발",
            "소프트웨어개발",
            "관제시스템",
            "관제장비",
            "스마트센서",
            "자동제어장치",
            "산업자동화",
            "제조자동화",
            "안전관리시스템",
            "의사결정시스템",
            "ICT융합",
            "ICT 융합"),
    NTIS_KEYWORDS(
            "스마트공장",
            "스마트제조",
            "디지털 트윈",
            "디지털트윈",
            "가상현실",
            "제조데이터",
            "자동제어",
            "IoT",
            "MES",
            "PLC",
            "AI",
            "인공지능",
            "ICTR&D혁신바우처",
            "ICT융복합",
            "ICT 융복합",
            "데이터 활용",
            "데이터 분석",
            "안전관리시스템",
            "의사결정시스템",
            "통합관제",
            "실시간 관제");

    private final List<KeywordPattern> keywords;

    BusanAxDxKeywordGroup(String... keywords) {
        this.keywords = Arrays.stream(keywords).map(KeywordPattern::new).toList();
    }

    public Optional<String> match(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return keywords.stream()
                .filter(keyword -> keyword.pattern().matcher(text).find())
                .map(KeywordPattern::keyword)
                .findFirst();
    }

    private static Pattern keywordPattern(String keyword) {
        String phrase = Pattern.quote(keyword).replace(" ", "\\E\\s+\\Q");
        return Pattern.compile(
                "(?<![\\p{IsHangul}A-Za-z0-9])" + phrase + "(?![\\p{IsHangul}A-Za-z0-9])",
                Pattern.CASE_INSENSITIVE);
    }

    private record KeywordPattern(String keyword, Pattern pattern) {

        private KeywordPattern(String keyword) {
            this(keyword, keywordPattern(keyword));
        }
    }
}
