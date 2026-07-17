# Tests

테스트 전략 문서와 보조 fixture를 두는 위치입니다.

## Java/Spring 테스트 기준

- Domain 로직은 Spring Context 없이 빠른 단위 테스트로 검증합니다.
- Controller는 `@WebMvcTest` 같은 slice test를 우선합니다.
- Repository 쿼리는 `@DataJpaTest` 또는 Testcontainers로 검증합니다.
- 전체 플로우는 필요한 만큼만 `@SpringBootTest`를 사용합니다.
- 테스트 이름은 조건과 기대 결과를 드러냅니다.
- 테스트 데이터가 서로 의존하지 않게 합니다.
- 테스트에서 운영 DB에 접속할 가능성을 차단합니다.

## DIVE 2026 테스트 기준

- KODATA 기업정보 다중 헤더를 올바르게 정규화하는지 테스트합니다.
- 0, 결측, 음수, KODATA 미매칭 상태를 구분하는지 테스트합니다.
- 2024년 지원의 후속연도 부재를 정상 상태로 처리하는지 테스트합니다.
- 지원 쌍 pair 기반 검토신호와 기업 단위 요약이 일치하는지 테스트합니다.
- 담당자 모드 응답과 기업 PoC 모드 응답 필드가 섞이지 않는지 테스트합니다.
- 금지 문구가 화면/API fixture에 포함되지 않는지 검사합니다.
