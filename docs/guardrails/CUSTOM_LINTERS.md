# CUSTOM_LINTERS

문서만으로 유지하기 어려운 취향과 규칙은 린트 또는 구조 테스트로 승격합니다.

후보:

- 파일 크기 제한
- 명명 규칙
- 구조화된 로깅
- 스키마/타입 배치
- 플랫폼 안정성 요구사항
- 금지된 의존성

## 권장 도구

- 포매팅: google-java-format 또는 Spotless
- 정적 분석: Checkstyle, Error Prone
- 아키텍처 검증: ArchUnit
- 테스트 커버리지와 품질: JUnit 5, AssertJ, Jacoco

## 자동화할 규칙 후보

- 필드 주입 금지
- Controller에서 Repository 의존 금지
- Entity에 `@Data` 사용 금지
- enum 필드에 `EnumType.STRING` 사용
- `catch (Exception e)` 후 무시 금지
- 민감 키워드 로그 출력 금지
- `@SpringBootTest` 사용 범위 제한
- 도메인 패키지의 Spring MVC 의존 금지
