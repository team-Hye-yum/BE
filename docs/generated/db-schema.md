# DB Schema

## Current ERD baseline

- Implement the attached DDL except the temporary table `Untitled4`.
- Include `company.industry_brief`, `company.ai_summary`, and `company.ai_one_line_summary` in the `company` table/entity.
- Keep API responses separated from JPA entities even when AI summary columns are stored on `company`.

이 문서는 Codex가 백엔드 구현을 시작할 때 우선 참조해야 하는 구현 대상 ERD입니다.

첨부 DDL에서 임시 테이블인 `Untitled4`는 제외합니다. 현재 ERD 기준으로는 `company.industry_brief`, `company.ai_summary`, `company.ai_one_line_summary`를 `company` 테이블/Entity에 포함합니다. 나머지 테이블은 우리가 구현할 1차 서비스 DB 모델로 취급합니다.

## 공통 원칙

- 테이블명은 첨부 DDL의 단수형 이름을 우선 유지한다.
- DDL의 오타성 타입 `VARHCAR`는 구현 시 `VARCHAR`로 수정한다.
- 기본키는 DDL의 `ALTER TABLE ... PRIMARY KEY` 정의를 따른다.
- 외래키가 DDL에 명시되어 있지 않아도 `company_id`는 `company.company_id`를 참조하는 논리적 조인 키로 다룬다.
- `btp_support_history.code`는 `btp_support_program.code`를 참조하는 논리적 프로그램 코드로 다룬다.
- 금액 단위는 컬럼명과 원천 주석의 단위를 함께 보존한다. 예: `support_amount`는 천원 단위.
- Boolean 원천값은 `Y/N`, `예/아니오`, `O/X`, `1/0`을 수용하되 DB에는 boolean으로 정규화한다.
- 날짜 원천값은 `yyyyMMdd`, `yyyy-MM-dd`를 모두 파싱하고 실패한 원문은 staging/품질 리포트에 남긴다.
- 사업자등록번호, 법인등록번호, 대표자명 등 민감 원문은 API 응답에 노출하지 않는다.

## 구현 대상 테이블

### `company`

기업 기본 프로필, 상태, 인증, 연구조직, 파생 지표를 담는 중심 테이블입니다.

기본키: `company_id`

구현 필드:

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `company_id` | INTEGER | 기업 일련번호, 전 테이블 조인 기준 |
| `region_name` | VARCHAR(20) | 지역 |
| `established_date` | DATE | 설립일자 |
| `business_entity_type` | VARCHAR(20) | 법인/개인 등 사업자 유형 |
| `company_size` | VARCHAR(20) | 소상공인/중소기업 등 규모 |
| `listing_status` | VARCHAR(20) | 상장/비상장 상태 |
| `company_type` | VARCHAR(20) | 주식회사/개인 등 형태 |
| `ksic_code` | VARCHAR(20) | KSIC 코드 |
| `industry_name` | VARCHAR(1000) | 업종명 |
| `industry_description` | TEXT | 업종 상세 |
| `main_product` | VARCHAR(1000) | 주요 제품 |
| `is_closed` | BOOLEAN | 휴폐업 여부 |
| `closed_date` | DATE | 휴폐업 일자 |
| `reference_date` | DATE | 조회/기준 일자 |
| `closure_type` | VARCHAR(20) | 폐업 구분 |
| `company_status` | VARCHAR(20) | 기업 상태 |
| `is_innobiz` | BOOLEAN | 이노비즈 여부 |
| `is_mainbiz` | BOOLEAN | 메인비즈 여부 |
| `is_venture_company` | BOOLEAN | 벤처기업 여부 |
| `is_materials_company` | BOOLEAN | 소재부품장비 여부 |
| `is_net_certified` | BOOLEAN | NET 인증 여부 |
| `is_nep_certified` | BOOLEAN | NEP 인증 여부 |
| `researcher_count` | INTEGER | 최근 연구자 수 |
| `has_research_lab` | BOOLEAN | 기업부설연구소 보유 여부 |
| `research_lab_registered_date` | DATE | 기업부설연구소 등록일 |
| `has_rnd_department` | BOOLEAN | 연구개발전담부서 보유 여부 |
| `rnd_department_registered_date` | DATE | 연구개발전담부서 등록일 |
| `debt_ratio` | REAL | 부채비율 파생 지표 |
| `cost_of_sales_ratio` | REAL | 매출원가율 파생 지표 |
| `sales_growth_rate` | REAL | 매출 성장률 |
| `employment_growth_rate` | REAL | 고용 성장률 |
| `government_rnd_dependency` | REAL | 정부 R&D 의존도 |
| `supported_sales_growth_rate` | REAL | 지원기업 매출 변화율 |
| `employment_peak_index` | REAL | 고용 관리 지표 |
| `employee_turnover_rate` | REAL | 고용 회전율 |
| `company_name` | VARCHAR(20) | 기업명, API 노출 시 마스킹/권한 확인 |
| `business_registration_number` | VARCHAR(20) | 사업자등록번호, 기본 API 노출 금지 |
| `address` | VARCHAR(1000) | 소재지 |
| `industry_brief` | TEXT | AI/context-generated industry brief |
| `ai_summary` | TEXT | AI-generated company summary |
| `ai_one_line_summary` | TEXT | AI-generated one-line company summary |


### `btp_support_program`

부산TP 지원사업 공고/프로그램 마스터입니다.

기본키: `code`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `code` | VARCHAR(20) | 사업 코드 |
| `program_year` | INTEGER | 사업 연도 |
| `budget_program_name` | VARCHAR(1000) | 예산서상 사업명 |
| `program_category` | VARCHAR(20) | 사업 구분 |
| `support_type` | VARCHAR(20) | 사업 유형 |
| `start_date` | DATE | 사업 시작일 |
| `end_date` | DATE | 사업 종료일 |
| `department_name` | VARCHAR(20) | 부처/부서명 |
| `local_government_name` | VARCHAR(20) | 지자체명 |
| `program_summary` | VARCHAR(1000) | 주요 내용 |
| `announcement_url` | VARCHAR(20) | 공고/PDF 링크, 길이 확장 필요 여부 검토 |

### `btp_support_history`

기업별 지원 이력입니다.

기본키: `support_history_id`

논리 관계:

- `company_id` -> `company.company_id`
- `code` -> `btp_support_program.code`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `support_history_id` | BIGINT | 지원 이력 ID |
| `support_year` | INTEGER | 지원 연도 |
| `code` | VARCHAR(20) | 사업 코드 |
| `budget_program_name` | VARCHAR(1000) | 원천 사업명 스냅샷 |
| `support_type` | VARCHAR(20) | 사업 유형 |
| `support_category` | VARCHAR(20) | 주요 지원 구분 |
| `support_detail` | VARCHAR(1000) | 추가 지원 구분 |
| `support_item` | VARCHAR(1000) | 지원 품목 원문 |
| `selected_date` | DATE | 선정일 |
| `selection_result` | VARCHAR(20) | 선정 결과 |
| `support_amount` | INTEGER | 지원금, 천원 단위 |
| `start_date` | DATE | 지원 시작일 |
| `end_date` | DATE | 지원 종료일 |
| `company_id` | INTEGER | 기업 일련번호 |
| `industry_code` | VARCHAR(20) | 업종 코드 |
| `province_name` | VARCHAR(20) | 광역 시도 |
| `district_name` | VARCHAR(20) | 기초 지자체 |
| `main_product` | VARCHAR(1000) | 원천 주요 생산품 |
| `established_year` | INTEGER | 설립 연도 |
| `Field` | VARCHAR(255) | 원천 잔여 필드, 용도 확인 전까지 API 노출 금지 |

### `company_financial_statistics`

기업별 연도 재무 통계입니다.

기본키: `financial_statistics_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `financial_statistics_id` | BIGINT | 재무 통계 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `year` | INTEGER | 기준 연도 |
| `sales_amount` | INTEGER | 매출액 |
| `operating_income` | INTEGER | 영업이익/손실 |
| `cost_of_sales` | INTEGER | 매출원가 |
| `net_income` | INTEGER | 당기순이익/손실 |
| `operating_margin` | REAL | 영업이익률 |
| `total_assets` | INTEGER | 자산총계 |
| `total_liabilities` | INTEGER | 부채총계 |
| `total_equity` | INTEGER | 자본총계 |
| `paid_in_capital` | INTEGER | 납입자본금 |
| `research_and_development_expense` | INTEGER | 연구개발비 |

### `company_employment_statistics`

기업별 연도 고용/국민연금 통계입니다.

기본키: `employment_statistics_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `employment_statistics_id` | BIGINT | 고용 통계 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `year` | INTEGER | 기준 연도 |
| `employee_counteld3` | INTEGER | 종업원 수. 구현 시 원천 호환명 유지, 도메인 DTO에서는 `employeeCount`로 노출 |
| `pension_subscriber_count` | INTEGER | 국민연금 가입자 수 |
| `pension_new_hire_count` | INTEGER | 국민연금 취업자 수 |
| `pension_retiree_count` | INTEGER | 국민연금 퇴직자 수 |
| `average_salary` | INTEGER | 평균 급여 |

### `company_patent`

기업 특허/실용신안 상세 이력입니다.

기본키: `patent_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `patent_id` | BIGINT | 특허 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `patent_type` | VARCHAR(20) | 특허권/실용신안 등 |
| `registration_status` | VARCHAR(20) | 등록 상태 |
| `application_date` | DATE | 출원일 |
| `registration_date` | DATE | 등록일 |
| `company_relation_code` | VARCHAR(20) | 본인/관계 등 |
| `is_active` | BOOLEAN | 유효 여부 |

### `company_patent_statistics`

기업별 연도 특허 집계입니다.

기본키: `patent_statistics_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `patent_statistics_id` | BIGINT | 특허 통계 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `year` | INTEGER | 기준 연도 |
| `registered_patent_count` | INTEGER | 등록 특허 건수 |
| `patent_application_count` | INTEGER | 특허 출원 건수 |

### `company_ntis_lead_project`

기업이 주관한 NTIS 과제 이력입니다.

기본키: `ntis_lead_project_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `ntis_lead_project_id` | BIGINT | NTIS 주관 과제 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `reference_year` | INTEGER | 기준 연도 |
| `reference_date` | DATE | 기준 일자 |
| `project_name` | VARCHAR(1000) | NTIS 사업/과제명 |
| `supervising_ministry_name` | VARCHAR(20) | 주관 부처명 |
| `region_name` | VARCHAR(1000) | 지역명 |
| `total_research_start_date` | DATE | 총 연구 시작일 |
| `total_research_end_date` | DATE | 총 연구 종료일 |
| `annual_research_start_date` | DATE | 당해 연구 시작일 |
| `annual_research_end_date` | DATE | 당해 연구 종료일 |
| `science_technology_category_name` | VARCHAR(1000) | 과학기술 표준분류 |
| `government_research_fund` | INTEGER | 정부 연구비 |
| `private_research_fund` | INTEGER | 민간 연구비 |
| `total_research_fund` | INTEGER | 총 연구비 |

### `company_ntis_collaborative_project`

기업이 공동/위탁 형태로 참여한 NTIS 과제 이력입니다.

기본키: `ntis_collaborative_project_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `ntis_collaborative_project_id` | BIGINT | NTIS 공동 과제 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `reference_year` | INTEGER | 기준 연도 |
| `reference_date` | DATE | 기준 일자 |
| `has_foreign_institute_collaboration` | BOOLEAN | 외국 연구기관 공동연구 여부 |
| `has_other_collaboration` | BOOLEAN | 기타 공동연구 여부 |
| `research_type_name` | VARCHAR(20) | 연구 형태명 |
| `collaboration_participation_type_name` | VARCHAR(20) | 공동연구 참여 형태명 |
| `collaboration_country_name` | VARCHAR(20) | 공동연구 참여 국가명 |
| `research_performer_type_name` | VARCHAR(20) | 연구 수행 주체명 |
| `commissioned_research_fund` | INTEGER | 위탁 과제 연구비 |
| `collaborative_research_expense` | INTEGER | 공동 연구비 |
| `collaborative_research_income` | INTEGER | 공동 연구비 수입금 |
| `has_company_collaboration` | BOOLEAN | 기업 공동연구 여부 |
| `has_university_collaboration` | BOOLEAN | 대학 공동연구 여부 |
| `has_public_institute_collaboration` | BOOLEAN | 공공기관 공동연구 여부 |

### `company_business_purpose`

법인 사업 목적 문장 목록입니다.

기본키: `business_purpose_id`

논리 관계: `company_id` -> `company.company_id`

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `business_purpose_id` | BIGINT | 사업 목적 ID |
| `company_id` | INTEGER | 기업 일련번호 |
| `display_order` | INTEGER | 표시 순서 |
| `business_purpose` | VARCHAR(1000) | 사업 목적 원문 |
| `registered_date` | DATE | 등기/등록 일자 |

## 관계 요약

```text
company 1 ── N btp_support_history
company 1 ── N company_financial_statistics
company 1 ── N company_employment_statistics
company 1 ── N company_patent
company 1 ── N company_patent_statistics
company 1 ── N company_ntis_lead_project
company 1 ── N company_ntis_collaborative_project
company 1 ── N company_business_purpose

btp_support_program 1 ── N btp_support_history
```

## 구현 체크리스트

- JPA Entity는 위 구현 대상 테이블만 만든다.
- `company.industry_brief`, `company.ai_summary`, `company.ai_one_line_summary`는 현재 ERD 기준에 따라 `company` Entity에 포함한다.
- Entity를 API 응답으로 직접 노출하지 않는다.
- `company_id` 기반 조회는 scorecard, support history, 재무/고용/특허/NTIS/사업목적을 필요에 따라 projection으로 묶는다.
- 연도별 테이블은 `(company_id, year)` 조회 인덱스를 둔다.
- 지원 이력은 `(company_id, support_year)`, `(code)`, `(selected_date)` 조회 인덱스를 검토한다.
- NTIS 과제는 `(company_id, reference_year)` 조회 인덱스를 둔다.
- 복합 유니크 제약:
  - `company_business_purpose(company_id, display_order)`
  - `company_employment_statistics(company_id, year)`
  - `company_financial_statistics(company_id, year)`
  - `company_patent_statistics(company_id, year)`
  - `btp_support_history(support_year, code)`
- 원천 DDL의 불명확 필드(`Field`, `employee_counteld3`)는 DB 호환을 위해 보존하되 도메인 DTO에서 의미 있는 이름으로 감싼다.

## Migration 초안 기준

첫 migration은 아래 순서로 작성한다.

1. 기준 테이블 `company`
2. 마스터 테이블 `btp_support_program`
3. 기업 하위 테이블 `btp_support_history`
4. 연도 통계 테이블 `company_financial_statistics`, `company_employment_statistics`, `company_patent_statistics`
5. 상세 이력 테이블 `company_patent`, `company_ntis_lead_project`, `company_ntis_collaborative_project`, `company_business_purpose`
6. 조회 인덱스

초기 데이터 품질이 확정되지 않은 상태에서는 물리 FK 제약을 유예할 수 있다. 단, 유예하더라도 migration 주석, Entity 관계 설계, Repository 조회 조건, 테스트명에는 논리 관계를 드러낸다.

## 타입 보정 기준

- 금액은 원천 단위를 확인할 때까지 `INTEGER`를 유지하되, Java에서는 `Long` 또는 `BigDecimal` 필요성을 검토한다.
- 비율은 DB `REAL`을 허용하되, 계산 정밀도가 중요한 서비스 로직에서는 `BigDecimal`을 검토한다.
- 날짜는 DB `DATE`, Java `LocalDate`를 사용한다.
- Boolean은 Java `Boolean`을 사용해 결측과 false를 구분한다.
- 원천 오타 컬럼 `employee_counteld3`는 DB 컬럼명을 유지하고 Java 필드명은 `employeeCount`로 매핑한다.
- `announcement_url`은 DDL상 `VARCHAR(20)`이지만 실제 URL 저장에는 부족할 가능성이 높다. 원천 길이를 확인하고 migration에서 확장 여부를 결정한다.
