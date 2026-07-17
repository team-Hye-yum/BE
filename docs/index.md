# 문서 인덱스

이 디렉터리는 Codex가 프로젝트 맥락, 데이터 계약, 구현 가드레일을 빠르게 읽기 위한 지식 베이스입니다.

## 먼저 읽을 문서

- `../AGENTS.md`: Codex 작업 순서와 즉시 차단 규칙
- `PROJECT_DOMAIN.md`: DIVE 2026 부산TP/KODATA 과제의 도메인 목표
- `generated/db-schema.md`: ai db를 제외한 구현 대상 ERD
- `DATA_MODELING.md`: 원천 데이터에서 서비스 DB/API 모델로 가는 규칙
- `ANALYSIS_RULES.md`: 변화율, 검토 신호, 근거 카드 작성 규칙
- `API_CONTRACT.md`: API 경로, 모드별 공개 범위, 응답 원칙
- `JAVA_SPRING_CODE_STYLE.md`: Java/Spring 백엔드 코드 스타일
- `EXECUTION_RUNBOOK.md`: 데이터 교체와 실행 절차

## 영역별 문서

- `generated/`: DB 스키마와 생성성 문서
- `product-specs/`: 제품 요구사항, 인수 기준, 사용자 피드백 변환
- `design-docs/`: 하네스 엔지니어링 원칙과 에이전트 가독성
- `guardrails/`: 아키텍처 규칙, 품질 게이트, 커스텀 린터
- `operations/`: 리뷰 루프, 병합 철학, 관측 가능성, 운영 리듬
- `references/`: LLM과 구현자가 참고할 외부/요약 레퍼런스
- `exec-plans/`: 실행 계획과 기술 부채 추적

## DB 작업 순서

1. `generated/db-schema.md`에서 구현 대상 테이블과 제외 대상을 확인한다.
2. `DATA_MODELING.md`에서 원천 파일, staging, service DB, serving 계층의 역할을 확인한다.
3. Entity, migration, Repository를 만들 때 AI 요약/추천/판단 저장 필드가 섞이지 않았는지 확인한다.
4. API 응답을 만들 때 Entity를 직접 반환하지 않고 DTO/projection으로 공개 범위를 분리한다.
5. 완료 전 `guardrails/QUALITY_GATES.md`와 `operations/REVIEW_LOOP.md` 기준으로 검증한다.
