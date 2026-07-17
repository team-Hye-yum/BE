# ENTROPY_GC

에이전트가 기존 패턴을 복제하면서 생기는 드리프트를 정기적으로 수거하는 운영 문서입니다.

정리 후보:

- 중복 유틸리티
- 오래된 문서
- 불균일한 명명
- 우회 구현
- 검증되지 않은 데이터 형태 추측
- 누락된 테스트와 관측 지점

## Java/Spring 정리 루프

매주 또는 주요 릴리스 전 다음 항목을 스캔합니다.

- Service 크기와 private 메서드 증가
- DTO와 Entity 경계 붕괴
- Controller 책임 증가
- Repository 쿼리 메서드 과도한 장문화
- `@SpringBootTest` 남발
- 공통 util 비대화
- 하드코딩된 설정값
- timeout 없는 외부 Client
- 표준 오류 응답에서 벗어난 코드
- 민감 정보 로그 가능성
