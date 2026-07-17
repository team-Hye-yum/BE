# Quality Gates

변경 전후 반드시 확인할 품질 게이트입니다. Codex는 구현을 마친 뒤 이 문서를 기준으로 검증 결과를 요약합니다.

## 공통 게이트

- 정적 분석 통과
- 단위 테스트 통과
- 필요한 slice/integration 테스트 통과
- 린트 또는 포맷 통과
- 구조 규칙 위반 없음
- 민감정보 로그/응답 노출 없음
- 문서 최신화

## Java/Spring PR 게이트

- ArchUnit 구조 테스트가 계층 의존 방향을 검증하는가?
- Controller가 Repository를 직접 호출하지 않는가?
- Controller가 Entity를 직접 반환하지 않는가?
- Service/Application public 메서드가 하나의 유스케이스를 표현하는가?
- 트랜잭션 범위가 public Service/Application 메서드에 있는가?
- 읽기 메서드에 `readOnly = true`가 적용되었는가?
- Repository 쿼리 테스트가 있는가?
- 목록 API에 pagination이 있는가?
- N+1 위험 API를 SQL 로그나 테스트로 확인했는가?
- 오류 응답 포맷이 표준을 따르는가?
- 권한별 DTO 공개 범위가 테스트되었는가?

## DB/Migration 게이트

- migration이 `docs/generated/db-schema.md`의 구현 대상 테이블만 만드는가?
- `company`의 AI 요약 컬럼은 포함하되, `Untitled4`와 별도 AI 결과 테이블이 없는가?
- `VARHCAR`가 `VARCHAR`로 수정되었는가?
- `company_id` 기반 논리 관계가 코드와 문서에 반영되었는가?
- `btp_support_history.code`와 `btp_support_program.code` 관계가 반영되었는가?
- 연도 통계 테이블에 `(company_id, year)` 인덱스가 있는가?
- 지원 이력 조회 인덱스가 있는가?
- 금액 단위와 날짜 파싱 기준이 테스트되었는가?
- 실제 0, 결측, 미매칭이 구분되는가?

## DIVE 2026 도메인 게이트

- 시스템이 기업을 자동 선정/탈락시키는 표현을 하지 않는가?
- 합격확률, 선정가능성, 자동 추천, 자동 알림 표현이 없는가?
- 지원 이후 변화를 성과나 인과관계로 단정하지 않는가?
- 평가위원용 `reviewerCheckpoints`가 기업 PoC 응답에 포함되지 않는가?
- 기업 PoC 응답에 내부 검토 항목이나 민감 원문이 노출되지 않는가?
- KODATA 미매칭을 `NOT_MATCHED` 또는 별도 상태로 표현하는가?
- `NO_SIGNAL`, `NOT_EVALUABLE`, `MISSING`, `NOT_COMPARABLE`을 구분하는가?
- 데이터 품질 리포트가 최신 fixture와 함께 갱신되었는가?

## Data Load 게이트

- 필수 시트/컬럼 존재 여부를 검증하는가?
- 다중 헤더와 연도 반복 컬럼을 명시적으로 매핑하는가?
- 날짜 파싱 실패를 품질 리포트에 남기는가?
- Boolean 파싱 실패를 조용히 null 처리하지 않는가?
- 숫자 콤마/공백 제거가 테스트되었는가?
- 원천 파일 경로와 민감 데이터가 로그에 남지 않는가?
- 적재 runId, sourceFileName, 성공/실패 건수, mappingVersion을 기록하는가?

## 완료 보고에 포함할 것

- 변경 파일
- 실행한 테스트/검증
- 실행하지 못한 테스트와 이유
- DB/migration 영향
- API 공개 범위 영향
- 남은 위험 또는 후속 작업
