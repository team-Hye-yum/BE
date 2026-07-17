# ARCHITECTURE

이 문서는 DIVE 2026 부산TP/KODATA 백엔드를 구현할 때의 최상위 구조 지침입니다.

## 목표

- 첨부 ERD에서 `ai db`를 제외한 서비스 DB를 안정적으로 구현한다.
- 평가위원이 기업 데이터와 지원 이력을 빠르게 검토할 수 있는 API를 제공한다.
- 시스템은 선정, 탈락, 합격 가능성을 자동 판단하지 않는다.
- 데이터 원천, 정제, 서비스 DB, API 응답을 분리한다.
- Codex가 이어서 작업해도 같은 구조와 판단 기준을 유지할 수 있게 문서와 가드레일을 남긴다.

## 기준 문서

DB 또는 백엔드 구현을 시작하기 전 반드시 아래 순서로 읽는다.

1. `AGENTS.md`
2. `docs/generated/db-schema.md`
3. `docs/DATA_MODELING.md`
4. `docs/API_CONTRACT.md`
5. `docs/JAVA_SPRING_CODE_STYLE.md`
6. `docs/guardrails/ARCHITECTURE_RULES.md`

## 패키지 구조

Java/Spring 백엔드는 도메인 단위 수직 분리를 기본값으로 둔다.

```text
com.hyehum.dive
├─ DiveApplication.java
├─ company
│  ├─ api
│  ├─ application
│  ├─ domain
│  └─ infra
├─ support
│  ├─ api
│  ├─ application
│  ├─ domain
│  └─ infra
├─ scorecard
│  ├─ api
│  ├─ application
│  └─ query
├─ review
│  ├─ api
│  ├─ application
│  └─ query
├─ applicant
│  ├─ api
│  ├─ application
│  └─ query
├─ dataload
│  ├─ application
│  ├─ parser
│  ├─ staging
│  └─ quality
├─ common
│  ├─ error
│  ├─ logging
│  ├─ security
│  └─ web
└─ config
```

## 도메인 책임

| 도메인 | 책임 |
|---|---|
| `company` | 기업 기본정보, 인증, 연구조직, 재무/고용/특허/NTIS/사업목적 조회의 중심 |
| `support` | 부산TP 지원사업 프로그램과 기업별 지원 이력 |
| `scorecard` | 재무, 고용, 인증, 특허, NTIS를 묶은 기업 요약 조회 |
| `review` | 평가위원 검토 화면용 근거 카드와 확인 필요 항목 |
| `applicant` | 기업 PoC 모드의 제한된 자기 점검 응답 |
| `dataload` | 원천 파일 파싱, staging, 매핑 검증, 품질 리포트 |
| `common` | 오류 응답, 보안, 로깅, 공통 웹 설정 |

`scorecard`, `review`, `applicant`는 쓰기 도메인이 아니라 조회 조립 도메인이다. 여러 테이블을 읽어 화면에 맞는 projection을 구성한다.

## Spring 계층 책임

| 계층 | 책임 | 금지 |
|---|---|---|
| `api` | HTTP 요청/응답, 인증 정보 해석, Bean Validation, 상태코드 결정 | Repository 직접 호출, Entity 직접 반환, 비즈니스 규칙 처리 |
| `application` | 유스케이스 조립, 트랜잭션 경계, 권한과 조회 범위 결정 | 모든 규칙을 private 메서드에 몰아넣기 |
| `domain` | 상태, 불변조건, 도메인 행위 | Spring MVC/DB/외부 API 의존 |
| `infra` | JPA Entity, Repository 구현, DB 접근 | 비즈니스 판단 |
| `query` | 화면/API 전용 조회 projection, N+1 회피용 쿼리 | 쓰기 로직 |
| `dataload` | 원천 파싱, 매핑, 품질 리포트 | 검증 실패 데이터의 조용한 무시 |

## 의존 방향

```text
api -> application -> domain
application -> repository/query port
infra/query -> repository/query port 구현
```

- `domain`은 Spring, JPA, HTTP를 모른다.
- `api`는 Entity를 모른다.
- `application`은 여러 도메인을 조립할 수 있지만, 순환 참조를 만들지 않는다.
- 조회 성능이 중요한 목록/상세 API는 Entity graph보다 명시적 projection을 먼저 검토한다.

## DB 구현 방침

구현 DB 기준은 `docs/generated/db-schema.md`다.

- Flyway 또는 Liquibase 같은 migration 도구를 사용한다.
- 첫 migration은 `ai db` 제외 테이블만 만든다.
- `VARHCAR` 같은 DDL 오타는 migration에서 `VARCHAR`로 수정한다.
- `company.industry_brief`, `company.ai_summary`, `company.ai_one_line_summary`, `Untitled4`는 migration에 넣지 않는다.
- DDL에 FK가 없더라도 논리 관계는 명확히 둔다.
- 실제 FK 제약은 초기 데이터 품질을 확인한 뒤 적용한다. 단, 애플리케이션 코드와 문서에서는 관계를 전제로 설계한다.
- 연도 통계와 지원 이력은 조회 인덱스를 migration에 함께 둔다.

## 데이터 파이프라인

```text
raw Excel / pasted DDL
-> staging parser
-> validation and mapping report
-> service DB
-> query projection
-> API DTO
```

원천 파일 구조를 API나 화면이 직접 알면 안 된다. 원천의 깨진 헤더, 다중 헤더, 오타 컬럼은 staging과 매핑 레이어에서 다루고, 서비스 API는 안정적인 DTO 이름을 사용한다.

## 조회 모델

기업 상세 화면은 `company` 단일 Entity 조회가 아니라 여러 read model의 조합이다.

- 기본 프로필: `company`
- 지원 이력: `btp_support_history` + `btp_support_program`
- 재무: `company_financial_statistics`
- 고용: `company_employment_statistics`
- 특허: `company_patent`, `company_patent_statistics`
- NTIS: `company_ntis_lead_project`, `company_ntis_collaborative_project`
- 사업목적: `company_business_purpose`

목록 API는 반드시 pagination을 가진다. 상세 API는 필요한 섹션을 명시하거나, endpoint를 분리해서 과도한 조인을 피한다.

## 보안과 공개 범위

- 기업명, 사업자등록번호, 주소는 민감정보로 취급한다.
- 기업 PoC 모드와 평가위원 모드는 DTO를 분리한다.
- 민감정보는 로그에 남기지 않는다.
- API 오류 응답에 원천 파일 경로, SQL, stack trace를 노출하지 않는다.
- 사업자등록번호는 검증된 조인 키가 아니다.

## 운영 가드레일

- 자동 선정, 자동 탈락, 합격확률, 선정가능성 문구를 만들지 않는다.
- AI 요약/추천/판단 결과 저장 DB를 만들지 않는다.
- 지원 이후 변화는 인과관계가 아니라 관찰 가능한 변화로만 표현한다.
- 결측, 0, 미매칭은 서로 다른 상태로 보존한다.
- 큰 변경은 문서, migration, 테스트를 같은 PR 단위로 묶는다.
