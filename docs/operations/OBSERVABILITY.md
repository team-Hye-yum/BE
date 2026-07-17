# OBSERVABILITY

Codex가 직접 확인할 수 있는 로그, 메트릭, 트레이스, 스크린샷, DOM 상태를 정의합니다.

기록할 것:

- 로컬 실행 방법
- 로그 조회 방법
- 주요 메트릭
- 핵심 사용자 여정
- 실패 재현 절차
- 검증 완료 기준

## Java/Spring 로그 기준

- 요청 ID, 사용자 ID, 도메인 ID를 가능한 구조화된 형태로 남깁니다.
- 정상 생성/변경 이벤트는 필요한 수준에서 `info`로 남깁니다.
- 상세 흐름은 `debug`로 둡니다.
- 예외는 `log.error("message. id={}", id, exception)` 형태로 스택트레이스를 보존합니다.
- 비밀번호, 토큰, API key, 개인식별정보는 로그에 남기지 않습니다.

## 관측 대상

- HTTP 요청 latency
- DB 쿼리 시간과 N+1 의심 패턴
- 외부 API latency, timeout, retry
- validation 실패율
- 인증/인가 실패율
- 주요 유스케이스 성공/실패 카운트

## DIVE 2026 관측 대상

- 데이터 로딩 성공/실패
- 시트/컬럼 누락
- 타입 변환 실패 건수
- KODATA 조인 성공률
- `NO_MATCH_IN_KODATA` 수
- `UNMAPPED` 지원품목 수
- `SIGNAL_FOUND` / `NO_SIGNAL` / `NOT_EVALUABLE` 분포
- 담당자 모드와 기업 모드 API 호출 분리
