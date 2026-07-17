# Harness Engineering Principles

하네스 엔지니어링은 Codex가 안정적으로 일할 수 있는 환경, 제약, 피드백 루프를 만드는 작업입니다.

## 구성 요소

- 작업을 쪼개는 계획 문서
- 재현 가능한 로컬 실행 환경
- 테스트와 구조 검증
- 로그, 메트릭, 트레이스 접근
- PR 리뷰와 후속 수정 루프
- 정기적인 문서 정리와 기술 부채 수거

## Java/Spring 하네스의 자동화 후보

- google-java-format 또는 Spotless
- Checkstyle
- Error Prone
- ArchUnit
- JUnit 5
- AssertJ
- Spring slice test
- Testcontainers
- SQL/N+1 관측 로그
