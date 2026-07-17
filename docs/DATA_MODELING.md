# Data Modeling

이 프로젝트의 구현 기준은 첨부 ERD에서 `ai db`를 제외한 서비스 DB입니다. Codex는 DB, Entity, Repository, migration, API DTO를 설계할 때 먼저 `docs/generated/db-schema.md`를 읽고 그 범위 안에서 작업합니다.

## 핵심 결정

- `company`를 중심 엔티티로 둔다.
- `company_id`는 모든 KODATA/BTP 파생 데이터의 1차 조인 키다.
- `btp_support_history.code`는 `btp_support_program.code`와 연결되는 사업 코드다.
- `company`의 AI 요약 필드는 현재 ERD 기준에 따라 포함하되, AI 분석 결과 저장용 별도 테이블은 1차 구현 범위가 아니다.
- 원천 파일의 모든 컬럼을 무조건 API/도메인 모델로 승격하지 않는다.
- 원천 스키마에 오타가 있어도 데이터 호환이 필요한 컬럼명은 DB에 남기고 DTO에서 의미 있는 이름으로 감싼다.

## 구현 범위

구현 대상 테이블:

- `company`
- `btp_support_program`
- `btp_support_history`
- `company_financial_statistics`
- `company_employment_statistics`
- `company_patent`
- `company_patent_statistics`
- `company_ntis_lead_project`
- `company_ntis_collaborative_project`
- `company_business_purpose`

제외 대상:

- `Untitled4`
- 별도 AI 요약, AI 추천, AI 판단 결과 저장 테이블

## 데이터 계층

| 계층 | 목적 | 저장/처리 방식 |
|---|---|---|
| raw file | 원천 엑셀/DDL 보존 | 파일 저장소 또는 읽기 전용 원본 |
| staging | 헤더, 타입, 날짜, Boolean 파싱 검증 | 임시 테이블 또는 중간 파일 |
| service DB | 첨부 ERD 기준의 구현 테이블 | `docs/generated/db-schema.md` 기준 |
| serving | 화면/API에 맞춘 응답 모델 | Entity가 아닌 DTO/projection |

staging은 원천을 보존하고 오류를 찾기 위한 계층입니다. 서비스 DB는 첨부 ERD를 기준으로 하되, `Untitled4` 제외 원칙과 타입 오타 수정 원칙을 적용합니다.

## 조인 기준

```text
BTP 지원 이력.company_id
== KODATA 기업정보.company_id
== 특허/NTIS/사업목적/재무/고용 통계.company_id
```

```text
BTP 지원 이력.code
== BTP 지원 프로그램.code
```

사업자등록번호는 조인 키로 확정하지 않습니다. 실제 매핑 제공 여부가 확인되기 전까지는 검색/입력 보조값으로만 다룹니다.

## 원천 파일 대응

### 부산TP 사업기업목록

주요 목적:

- `btp_support_program` 생성
- `btp_support_history` 생성

처리 규칙:

- 연도별 시트는 `program_year` 또는 `support_year`로 정규화한다.
- 지원금은 천원 단위로 저장한다.
- 지원 품목 원문은 `support_item`에 보존한다.
- `selection_result`는 단순 합격/불합격 판단으로 확대 해석하지 않는다.

### KODATA 기업데이터

주요 목적:

- `company` 생성/보강
- `company_financial_statistics` 생성
- `company_employment_statistics` 생성
- `company_patent`, `company_patent_statistics` 생성
- `company_ntis_lead_project`, `company_ntis_collaborative_project` 생성
- `company_business_purpose` 생성

처리 규칙:

- 다중 헤더는 staging에서 해석한 뒤 서비스 DB 컬럼으로 매핑한다.
- 연도 반복 재무/고용/특허 집계는 각각의 통계 테이블에 행 단위로 저장한다.
- 특허 상세와 특허 통계는 섞지 않는다.
- NTIS 주관 과제와 공동/위탁 과제는 별도 테이블로 유지한다.
- 법인 사업 목적은 표시 순서와 원문을 보존한다.

## 정제 규칙

- DB 컬럼은 snake_case, Java 필드는 camelCase를 사용한다.
- DDL의 `VARHCAR`는 `VARCHAR`로 수정한다.
- 날짜는 `yyyyMMdd`, `yyyy-MM-dd`를 모두 허용한다.
- 숫자 문자열의 콤마와 공백은 제거한 뒤 파싱한다.
- 실제 0과 결측은 구분한다.
- 결측 사유를 판단할 수 있으면 품질 리포트에 남긴다.
- Boolean 원천값은 정규화하되, 알 수 없는 값은 실패로 기록한다.
- 민감 원문은 로그와 API 응답에 남기지 않는다.

## API 모델링 원칙

- Entity를 직접 반환하지 않는다.
- 기업 상세 응답은 필요한 하위 정보를 명시적으로 선택해서 구성한다.
- 평가위원 모드와 기업 PoC 모드는 응답 필드 공개 범위를 분리한다.
- AI가 만든 판단처럼 보이는 필드는 추가하지 않는다.
- "추천", "선정 가능성", "합격 확률" 같은 표현은 API와 화면에서 사용하지 않는다.
- 근거 데이터, 변화율, 검토 신호, 확인 필요 항목 중심으로 응답한다.

## 인덱스 기준

우선 검토 인덱스:

- `btp_support_history(company_id, support_year)`
- `btp_support_history(code)`
- `btp_support_history(selected_date)`
- `company_financial_statistics(company_id, year)`
- `company_employment_statistics(company_id, year)`
- `company_patent(company_id, registration_date)`
- `company_patent_statistics(company_id, year)`
- `company_ntis_lead_project(company_id, reference_year)`
- `company_ntis_collaborative_project(company_id, reference_year)`
- `company_business_purpose(company_id, display_order)`

## 금지

- `company` 외부에 AI 요약/추천/판단 결과 저장 테이블 추가하기
- 원천 임시 테이블 `Untitled4` 구현하기
- 원천 컬럼 전체를 그대로 API 응답에 노출하기
- 결측을 0으로 치환하기
- KODATA 미매칭을 정상 0값처럼 표시하기
- 사업자등록번호를 검증된 조인 키처럼 사용하기
- 지원 이후 변화를 지원 효과 또는 인과관계로 단정하기
