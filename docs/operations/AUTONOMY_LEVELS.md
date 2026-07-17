# AUTONOMY_LEVELS

Codex에게 위임할 수 있는 자율성 수준을 정의합니다.

- Level 1: 조사와 제안
- Level 2: 구현 후 사람 검토
- Level 3: 구현, 검증, PR 작성
- Level 4: 피드백 반영과 빌드 실패 복구
- Level 5: 제한된 조건에서 병합까지 수행

## Java/Spring 위임 기준

- Level 1: 기존 구조 조사, 영향 범위 분석, 실행 계획 작성
- Level 2: 단일 Controller/Service/Repository 변경 구현
- Level 3: 도메인 규칙, 테스트, 오류 응답, 문서까지 포함한 기능 구현
- Level 4: 리뷰 코멘트 반영, ArchUnit/정적 분석 실패 수정, flaky test 원인 분석
- Level 5: 사전에 정의된 품질 게이트를 모두 통과한 작은 변경 자동 병합

보안, 결제, 개인정보, 대규모 마이그레이션은 Level 5로 위임하지 않습니다.
