# API Contract

이 문서는 DIVE 2026 백엔드 API의 공개 범위와 응답 원칙을 정의합니다. API는 Entity를 직접 반환하지 않고, 평가위원 모드와 기업 PoC 모드의 DTO를 분리합니다.

## 원칙

- API는 기업을 자동 선정하거나 탈락시키지 않는다.
- `recommend`, `passProbability`, `selectionChance` 같은 필드를 만들지 않는다.
- 상태 코드, 근거 데이터, 기준 연도, 원천 출처, 결측 상태를 명확히 반환한다.
- Entity, 원천 컬럼 전체, 민감 원문을 그대로 반환하지 않는다.
- AI 요약/추천/판단 저장 필드는 응답 계약에 포함하지 않는다.
- 목록 응답은 pagination을 기본으로 한다.

## 인증 모드

| 모드 | 용도 | 공개 범위 |
|---|---|---|
| `REVIEWER` | 평가위원/기관 담당자 검토 | 근거 카드, 지원 이력, 검토 신호, 제한된 민감정보 |
| `APPLICANT_POC` | 기업 자기 점검 PoC | 공개 가능한 요약, 제출 전 확인 항목, 민감정보 최소화 |
| `ADMIN` | 데이터 적재/품질 관리 | 품질 리포트, 매핑 실패, 적재 상태 |

권한이 없으면 필드를 null로 숨기는 방식보다 DTO 자체를 분리한다.

## 주요 Endpoint

```text
GET /api/companies
GET /api/companies/{companyId}
GET /api/companies/{companyId}/scorecard
GET /api/companies/{companyId}/supports
GET /api/companies/{companyId}/financial-statistics
GET /api/companies/{companyId}/employment-statistics
GET /api/companies/{companyId}/patents
GET /api/companies/{companyId}/ntis-projects
GET /api/companies/{companyId}/business-purposes
GET /api/companies/{companyId}/review-summary

GET /api/support-programs
GET /api/support-programs/{code}

GET /api/applicant/companies/{companyId}/precheck

GET /api/admin/data-quality/latest
GET /api/admin/data-load-runs/{runId}
```

## 목록 조회

`GET /api/companies`

Query:

| 이름 | 설명 |
|---|---|
| `keyword` | 기업명 또는 제한된 검색어. 사업자등록번호 검색은 권한 확인 후 별도 처리 |
| `regionName` | 지역 |
| `ksicCode` | KSIC 코드 |
| `companySize` | 기업 규모 |
| `page` | 0부터 시작 |
| `size` | 기본 20, 최대 100 |
| `sort` | 허용된 정렬 키만 사용 |

Response 예시:

```json
{
  "items": [
    {
      "companyId": 117,
      "companyName": "마스킹 또는 권한 기반 표시",
      "regionName": "부산",
      "ksicCode": "C21309",
      "industryName": "의료용품 제조업",
      "companySize": "소상공인",
      "companyStatus": "정상"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

## 기업 상세

`GET /api/companies/{companyId}`

기본 프로필만 반환한다. 지원 이력, 재무, 고용, 특허, NTIS는 별도 endpoint로 분리해 과도한 조인을 피한다.

민감정보:

- `businessRegistrationNumber`는 기본 응답에 포함하지 않는다.
- `address`는 권한과 목적이 확인된 경우에만 전체 표시한다.
- 기업 PoC 모드에서는 기관 내부 검토용 필드를 포함하지 않는다.

## Scorecard

`GET /api/companies/{companyId}/scorecard`

여러 테이블을 읽어 평가 근거용 요약을 구성한다.

```json
{
  "companyId": 117,
  "baseYears": [2022, 2023, 2024],
  "financial": {
    "salesAmount": {
      "status": "OBSERVED",
      "value": 28018841,
      "unit": "KRW_THOUSAND",
      "year": 2024,
      "source": "company_financial_statistics"
    }
  },
  "employment": {
    "employeeCount": {
      "status": "OBSERVED",
      "value": 12,
      "unit": "PERSON",
      "year": 2024,
      "source": "company_employment_statistics"
    }
  },
  "certifications": {
    "innobiz": true,
    "mainbiz": false,
    "ventureCompany": true
  }
}
```

## 지원 이력

`GET /api/companies/{companyId}/supports`

`btp_support_history`를 기준으로 하며, 가능한 경우 `btp_support_program`을 조인해 프로그램 정보를 보강한다.

```json
{
  "companyId": 117,
  "items": [
    {
      "supportHistoryId": 1,
      "supportYear": 2022,
      "programCode": "B1_311",
      "programName": "사업명",
      "supportType": "사업화지원",
      "supportItem": "지원 품목 원문",
      "selectedDate": "2022-08-26",
      "selectionResult": "지원대상",
      "supportAmount": {
        "value": 13400,
        "unit": "KRW_THOUSAND"
      },
      "period": {
        "startDate": "2022-09-01",
        "endDate": "2023-03-31"
      }
    }
  ]
}
```

## 검토 신호

검토 신호는 판단이 아니라 사람이 확인할 관찰 항목이다.

상태값:

| 상태 | 의미 |
|---|---|
| `OBSERVED` | 원천 데이터가 있고 계산 가능 |
| `MISSING` | 원천 데이터 결측 |
| `NOT_MATCHED` | KODATA/BTP 조인 실패 |
| `NOT_COMPARABLE` | 기준 연도나 단위가 달라 비교 불가 |
| `NOT_EVALUABLE` | 필요한 필드 부족 |

`reviewerCheckpoints`와 `applicantPrecheckItems`는 섞지 않는다.

```json
{
  "companyId": 117,
  "reviewerCheckpoints": [
    {
      "code": "POST_SUPPORT_SALES_CHANGE",
      "status": "OBSERVED",
      "message": "지원 이후 매출 변화가 관찰되었습니다.",
      "sourceFields": [
        "btp_support_history.start_date",
        "company_financial_statistics.sales_amount"
      ],
      "ruleVersion": "review-signal-v1"
    }
  ]
}
```

## 오류 응답

```json
{
  "code": "COMPANY_NOT_FOUND",
  "message": "기업 정보를 찾을 수 없습니다.",
  "traceId": "01H..."
}
```

HTTP 상태:

- `400`: 요청 형식/검증 실패
- `401`: 인증 실패
- `403`: 권한 없음
- `404`: 리소스 없음
- `409`: 중복 또는 상태 충돌
- `422`: 파일 적재/매핑 검증 실패
- `500`: 서버 오류

## 응답 금지

- 사업자등록번호, 법인등록번호, 대표자명 원문
- 원천 민감 컬럼 전체
- stack trace, SQL, 내부 파일 경로
- 평가위원 확인 질문을 기업 PoC 모드에 포함
- 합격확률, 선정가능성, 자동 추천, 자동 알림
- AI 요약/추천/판단 결과 필드
