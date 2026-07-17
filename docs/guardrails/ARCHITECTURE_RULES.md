# Architecture Rules

이 문서는 아키텍처 불변조건과 차단 규칙을 정의합니다. 구현 중 아래 규칙과 충돌하면 코드를 늘리기 전에 설계를 고칩니다.

## 불변조건

- 구현 DB 기준은 `docs/generated/db-schema.md`다.
- `ai db` 제외 원칙을 지킨다.
- API는 Entity를 직접 반환하지 않는다.
- 원천 파일 구조를 API 계약으로 노출하지 않는다.
- 평가위원 모드와 기업 PoC 모드의 DTO를 분리한다.
- 결측, 0, 미매칭을 구분한다.
- 지원 이후 변화는 인과관계로 표현하지 않는다.

## 의존 방향

- `api`는 `application`만 호출한다.
- `application`은 `domain`과 port/query를 사용한다.
- `domain`은 Spring, JPA, HTTP, 외부 API를 모른다.
- `infra`는 repository/client adapter 구현을 담당한다.
- 도메인 간 직접 참조는 명시적 application 유스케이스나 port를 통해서만 허용한다.

## Java/Spring 규칙

- Controller는 Repository를 직접 호출하지 않는다.
- Controller는 Entity를 직접 반환하지 않는다.
- Service/Application public 메서드는 하나의 유스케이스를 표현한다.
- 트랜잭션은 public Service/Application 메서드에 둔다.
- Repository에는 비즈니스 규칙을 넣지 않는다.
- Entity setter는 제한하고 의미 있는 상태 변경 메서드를 사용한다.
- 외부 입력 DTO는 도메인 모델과 분리한다.
- 목록 API는 pagination을 가진다.
- 조회 전용 복합 API는 projection/query model을 우선한다.

## DB 규칙

- migration에는 `company.industry_brief`, `company.ai_summary`, `company.ai_one_line_summary`를 `company`에 포함하고, `Untitled4`는 만들지 않는다.
- `VARHCAR`는 `VARCHAR`로 수정한다.
- `company_id`는 기업 관련 테이블의 기준 조인 키다.
- `btp_support_history.code`는 `btp_support_program.code`와 연결한다.
- 연도 통계 테이블은 `(company_id, year)` 조회 인덱스를 둔다.
- 지원 이력은 `(company_id, support_year)`, `(code)` 인덱스를 검토한다.
- FK 제약을 유예하더라도 애플리케이션과 문서의 논리 관계는 유지한다.
- 원천 오타 컬럼은 필요한 경우 DB에 보존하되 DTO에서 의미 있는 이름으로 감싼다.

## API 금지

- 사업자등록번호, 법인등록번호, 대표자명 원문 노출
- stack trace, SQL, 내부 파일 경로 노출
- Entity 필드 전체를 그대로 내려주는 응답
- 평가위원 확인 항목을 기업 PoC 모드에 포함
- 합격확률, 선정가능성, 자동 추천, 자동 알림
- AI 요약/추천/판단 결과 필드

## 즉시 중단할 신호

아래가 보이면 구현을 멈추고 문서 또는 설계를 먼저 수정한다.

- `company` 외부에 AI 요약/추천/판단 결과 저장 컬럼이나 테이블 추가
- 원천 엑셀 헤더를 그대로 API 필드로 사용
- `company_id`가 아닌 사업자등록번호를 기본 조인 키로 사용
- 결측을 0으로 치환
- KODATA 미매칭을 정상 데이터처럼 표시
- 지원 이후 변화 문구가 성과나 효과를 단정
- Controller가 SQL 성능 문제를 감춘 채 복합 Entity 그래프를 반환

## 검증 방식

- ArchUnit으로 계층 의존 방향을 검증한다.
- Repository 쿼리는 `@DataJpaTest`로 검증한다.
- API 공개 범위는 권한별 계약 테스트로 검증한다.
- migration 파일은 금지 컬럼/테이블 문자열을 검사하는 lightweight lint를 둔다.
- 데이터 적재는 샘플 파일과 실패 fixture를 함께 테스트한다.
