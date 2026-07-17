# Analysis Rules

## 공통 원칙

- 분석 결과는 판정이 아니라 검토 신호입니다.
- 모든 수치와 신호에는 출처와 상태 코드를 남깁니다.
- 문구보다 구조화된 상태값을 먼저 반환합니다.
- 화면 문구는 상태값, 출처, 기준연도, 근거 필드를 조합해 만듭니다.
- 상태값은 종류별 네임스페이스를 섞지 않습니다. 변화율의 `NOT_COMPARABLE`과 검토신호의 `NOT_EVALUABLE`은 다른 의미입니다.

## 결측치

실제 0과 데이터 없음은 다릅니다.

- KODATA 미매칭: `NO_MATCH_IN_KODATA`
- 전년 값 없음: `PREVIOUS_YEAR_MISSING`
- 당해 값 없음: `CURRENT_YEAR_MISSING`
- 후속 연도 없음: `FOLLOWUP_YEAR_UNAVAILABLE`
- 필수 필드 부족: `NOT_EVALUABLE`

## 변화율 상태

| 상태 | 조건 | 표시 |
|---|---|---|
| `COMPARABLE_RATE` | 전년 값과 당해 값이 모두 있고 전년 값이 양수 | 비율과 절대 변화량 |
| `COMPARABLE_ABSOLUTE_ONLY` | 두 값은 있으나 전년 값이 0 또는 음수 | 절대 변화량만 |
| `NOT_COMPARABLE` | 전년/당해 값 중 하나가 없거나 KODATA 미매칭 | 사유 표시 |

기본식:

```text
(당해 값 - 전년 값) / 전년 값
```

음수나 0 기준값에 억지 비율을 붙이지 않습니다.

## 지원 이후 변화

- 지원연도 수치는 동시기 데이터로만 표시합니다.
- 지원 종료 다음 연도부터 지원 이후 관찰값으로 봅니다.
- 종료일이 없거나 진행 중이면 선정연도 다음 연도를 기준으로 사용합니다.
- 2024년 지원은 2025년 KODATA 데이터가 없으므로 후속 변화 확인이 불가할 수 있습니다.
- 항상 “인과관계는 판단하지 않음”을 UI/API에 포함합니다.

## 복수 지원

같은 해 복수 지원은 특정 사업 하나에 변화를 귀속하지 않습니다.

권장 문구:

```text
2023년 복수 지원 이후 기업에서 관찰된 변화
```

금지 문구:

```text
이 지원 덕분에 매출이 증가했습니다.
```

## 검토신호

신호는 기업 전체가 아니라 지원 쌍(pair) 단위로 먼저 계산합니다.

## 지원 쌍 모델

```json
{
  "fromSupportId": "S2022-001",
  "toSupportId": "S2023-002",
  "linkageSignal": true,
  "repetitionSignal": false,
  "sameFiscalYear": false,
  "linkageReasons": ["TECH_TO_COMMERCIALIZATION"],
  "repetitionReasons": [],
  "ruleVersion": "support-signal-v1",
  "itemMappingVersion": "support-item-map-v1"
}
```

기업 단위 요약은 pair의 OR 집계로 만들되, 근거 문구를 만들 수 있도록 관련 pair ID를 함께 남깁니다.

### 상태

- `SIGNAL_FOUND`: 연계 또는 반복 신호가 하나 이상 있음
- `NO_SIGNAL`: 필요한 데이터는 충분하지만 신호가 없음
- `NOT_EVALUABLE`: 시작일, 사업유형 등 필수 데이터 부족으로 계산 불가

### 독립 신호

- `linkageSignal`: 예를 들어 기술지원 이후 사업화지원
- `repetitionSignal`: 유사 목적 지원이 12개월 이내 반복되는 경우
- 두 신호는 동시에 true일 수 있습니다.

### 비교 범위 v1

- 연계 신호: 시간순 인접 지원끼리 비교
- 반복 신호: 12개월 이내 모든 지원 쌍 비교
- 동일 회계연도 여부는 별도 필드 `sameFiscalYear`로 저장

## 지원품목 정규화

- 1차: 사업유형, 지원구분 코드 완전 일치
- 2차: 지원품목 텍스트를 수작업 사전으로 카테고리화
- 사전에 없는 값은 `UNMAPPED`로 둡니다.
- 원문 `rawItem`은 항상 보존합니다.
- `itemMappingVersion`을 별도로 관리합니다.

## 버전

```json
{
  "ruleVersion": "support-signal-v1",
  "itemMappingVersion": "support-item-map-v1"
}
```

규칙 파라미터가 바뀌면 `ruleVersion`을 올립니다. 정규화 사전만 바뀌면 `itemMappingVersion`만 올립니다.

## 평가 근거 카드

카드는 코드와 템플릿이 조립합니다.

포함:

- 기업 기본 프로필
- 핵심 지표 3~4개
- 지원이력과 검토신호
- 지원 이후 관찰 변화
- 근거 출처
- 평가위원 확인 질문 제안
- “최종 판단은 평가위원이 합니다”

## 출처 표기

출처는 자연어 문자열만 두지 말고 코드와 표시명을 함께 관리합니다.

| sourceCode | 표시명 |
|---|---|
| `BUSAN_TP_SUPPORT_LIST` | 부산TP 기업지원목록 |
| `BUSAN_TP_PROGRAM_LIST` | 부산TP 사업목록 |
| `KODATA_COMPANY_INFO` | KODATA 기업정보 |
| `KODATA_PATENT` | KODATA 특허및실용신안 |
| `KODATA_NTIS_MAIN` | KODATA NTIS 주관 |
| `KODATA_NTIS_CONSIGNMENT` | KODATA NTIS 위탁 |
| `KODATA_CORPORATE_PURPOSE` | KODATA 법인사업목적 |

## AI 사용 제한

AI가 해도 되는 것:

- 문장 길이 축약
- 중복 문장 제거
- 발표용 자연어 다듬기
- 기업용 제출 전 확인사항 초안 생성

AI가 하면 안 되는 것:

- 선정/탈락 판단
- 성공확률 계산
- 핵심 수치 생성
- 근거 없는 추천
- 검토신호 자유 생성
- 인과관계 주장
