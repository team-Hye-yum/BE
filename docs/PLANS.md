# Plans

작은 변경은 대화 안의 계획으로 충분하지만, DB, API, 데이터 적재, 권한, 복합 조회가 얽힌 변경은 `docs/exec-plans/` 아래에 실행 계획을 남깁니다.

## 실행 계획 원칙

- 목표와 비범위를 먼저 적는다.
- 현재 구조와 변경 구조를 구분한다.
- DB/migration, API, 테스트, 문서 영향을 함께 적는다.
- `ai db` 제외 규칙 위반 여부를 명시적으로 확인한다.
- 결정 로그를 남겨 다음 Codex 작업자가 같은 논리를 이어받게 한다.

## 실행 계획 템플릿

```md
# 작업명

## 목표

## 비범위

- AI 요약/추천/판단 DB 구현 여부:
- 자동 선정/탈락 기능 여부:

## 현재 구조

## 변경 계획

## DB/Migration 영향

- 생성/수정 테이블:
- 인덱스:
- FK 또는 논리 관계:
- 제외 확인:
  - company.industry_brief:
  - company.ai_summary:
  - company.ai_one_line_summary:
  - Untitled4:

## API 영향

- Endpoint:
- Request:
- Response:
- 평가위원 모드 공개 범위:
- 기업 PoC 모드 공개 범위:
- 민감정보 처리:

## Java/Spring 기준 확인

- Controller 책임:
- Service/Application 책임:
- Domain 규칙 위치:
- Repository/JPA 영향:
- Query/projection 필요 여부:
- Transaction 범위:
- Exception/API 오류:
- Logging/Security:

## DIVE 2026 기준 확인

- 자동 선정/탈락처럼 보이는 표현:
- 합격확률/선정가능성 표현:
- 사업자등록번호 조인 키 사용 여부:
- 결측/0/KODATA 미매칭 처리:
- 지원 이후 변화의 인과 표현 여부:
- 데이터 품질 리포트 영향:

## 검증

- 단위 테스트:
- Repository 테스트:
- API 계약 테스트:
- 통합 테스트:
- 정적 분석:
- 수동 확인:

## 결정 로그

## 완료 조건
```
