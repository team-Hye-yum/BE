# AGENTS.md

이 파일은 Codex가 이 저장소에서 작업할 때 가장 먼저 읽는 지침입니다.

## 우선 읽을 문서

- 전체 구조: `ARCHITECTURE.md`
- 문서 인덱스: `docs/index.md`
- 프로젝트 도메인: `docs/PROJECT_DOMAIN.md`
- 구현 DB/ERD 기준: `docs/generated/db-schema.md`
- 데이터 모델링: `docs/DATA_MODELING.md`
- 분석 규칙: `docs/ANALYSIS_RULES.md`
- API 계약: `docs/API_CONTRACT.md`
- Java/Spring 코드 스타일: `docs/JAVA_SPRING_CODE_STYLE.md`
- 실행 계획: `docs/PLANS.md`
- 품질 기준: `docs/QUALITY_SCORE.md`
- 안정성 기준: `docs/RELIABILITY.md`
- 보안 기준: `docs/SECURITY.md`
- 아키텍처 가드레일: `docs/guardrails/ARCHITECTURE_RULES.md`
- 리뷰 루프: `docs/operations/REVIEW_LOOP.md`
- 관측 가능성: `docs/operations/OBSERVABILITY.md`

## 현재 구현 기준

- 첨부 ERD에서 `ai db`를 제외한 테이블이 1차 구현 대상이다.
- 구현 대상 ERD는 `docs/generated/db-schema.md`에 정리되어 있다.
- `company.industry_brief`, `company.ai_summary`, `company.ai_one_line_summary`는 현재 ERD 기준에 따라 `company`에 구현한다.
- 임시 테이블 `Untitled4`는 구현하지 않는다.
- AI 추천, AI 판단 결과를 저장하는 별도 DB 모델은 만들지 않는다.
- DDL의 `VARHCAR`는 구현 시 `VARCHAR`로 바로잡는다.
- `company_id`는 모든 기업 관련 데이터의 기준 조인 키다.
- `btp_support_history.code`는 `btp_support_program.code`와 연결한다.

## 작업 원칙

- 목표를 설계, 구현, 검증, 리뷰 단위로 쪼갠다.
- 중요한 판단은 문서에 남긴 규칙으로 저장소에 흡수한다.
- 반복되는 리뷰 코멘트는 문서 또는 lint/테스트 규칙으로 옮긴다.
- 작업 완료 전 테스트, 로그, 문서 영향 범위를 확인한다.
- Entity를 API 응답으로 직접 노출하지 않는다.
- 원천 데이터와 서비스 도메인 모델을 구분한다.

## Codex 작업 순서

1. DIVE 2026 기능, 데이터, 화면 문구, API 작업이면 `docs/PROJECT_DOMAIN.md`를 먼저 확인한다.
2. DB, Entity, migration, Repository, 데이터 정제 작업이면 `docs/generated/db-schema.md`와 `docs/DATA_MODELING.md`를 먼저 확인한다.
3. 검토 신호, 변화율, 스코어카드, 근거 카드가 나오면 `docs/ANALYSIS_RULES.md`를 확인한다.
4. API 응답, FE 연동, 평가위원/기업 모드가 나오면 `docs/API_CONTRACT.md`를 확인한다.
5. 구현 전 바뀌는 계약과 테스트 범위를 `docs/PLANS.md` 기준으로 정리한다.
6. 완료 전 `docs/guardrails/QUALITY_GATES.md`와 `docs/operations/REVIEW_LOOP.md`를 대조한다.

## Java/Spring 기본값

- Java 21 LTS와 Spring Boot 기반 백엔드를 기본 가정으로 둔다.
- Controller는 얇게 유지한다.
- Service/Application 계층은 유스케이스 조립자로 둔다.
- Domain은 상태와 규칙의 소유자로 둔다.
- 필수 의존성은 생성자 주입과 `final` 필드로 표현한다.
- 트랜잭션은 public Service/Application 메서드의 유스케이스 단위에 둔다.
- 예외, validation, API 응답, 로그, 보안은 표준 포맷과 공통 경계에서 처리한다.
- 스타일 일관성은 정적 분석, ArchUnit, 테스트로 자동화한다.

## DIVE 2026 도메인 기본값

- 이 프로젝트는 부산TP/KODATA 기업데이터 기반 선정 보조 대시보드다.
- 시스템은 기업을 자동 선정하거나 탈락시키지 않는다.
- 시스템은 평가 근거를 사람이 쉽게 이해하도록 정리한다.
- 최종 판단은 해당 기관의 평가위원이 한다.
- 지원 이후 변화는 지원 효과나 인과관계로 단정하지 않는다.
- 현재 확정 가능한 결합 키는 `company_id`다.
- 사업자등록번호 입력은 실제 매핑 제공 여부가 확인되기 전까지 확정 기능처럼 표현하지 않는다.
- 기업명, 사업자등록번호, 주소 등 민감 데이터는 권한과 마스킹 기준 없이 노출하지 않는다.

## 즉시 차단 규칙

아래가 보이면 구현을 계속하지 말고 설계나 문구를 먼저 고친다.

- AI 추천/판단 결과 저장 컬럼 또는 테이블 추가
- `company` 외부에 AI 요약 저장용 별도 테이블 추가
- `Untitled4` 구현
- 원천 컬럼을 그대로 영구 DB/API 모델로 노출
- 결측, 0, KODATA 미매칭을 같은 값으로 처리
- 사업자등록번호를 검증 없이 조인 키로 사용
- 기업을 자동 선정/탈락시키는 문구 또는 기능
- 합격확률, 선정가능성, 자동 알림, 자동 추천 문구
- 지원 이후 변화를 지원 효과 또는 인과관계처럼 표현
