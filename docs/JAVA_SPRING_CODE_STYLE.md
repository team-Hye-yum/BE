# Java/Spring Code Style

이 문서는 Java 21 LTS와 Spring Boot 기반 백엔드를 구현할 때의 코드 스타일 기준입니다. 목표는 멋진 코드보다 다음 사람이 안전하게 바꿀 수 있는 코드입니다.

## 철학

- 예측 가능성, 변경 용이성, 테스트 가능성, 운영 안정성을 우선한다.
- 스타일은 정적 분석과 테스트로 자동화한다.
- 기존 코드베이스의 패턴을 우선한다.
- 작은 유스케이스를 명확히 쌓고, 공통화는 반복이 충분히 보일 때 한다.

## Java 기본값

- Java 21 LTS를 기본으로 둔다.
- 이름은 구현 방식보다 의도를 먼저 드러낸다.
- 의미 없는 `data`, `info`, `temp`, `result`, `list` 이름을 피한다.
- `Manager`, `Processor`, `Handler`는 책임이 명확할 때만 쓴다.
- 메서드는 하나의 추상화 수준을 유지한다.
- `var`는 타입이 명확한 지역 변수에만 제한적으로 사용한다.
- `Optional`은 반환 타입 중심으로 사용하고 필드와 파라미터에는 쓰지 않는다.
- Stream은 순수 변환 파이프라인에 사용하고, 부수효과가 많으면 for문을 선호한다.

## Spring 구조

- Controller는 얇게 유지한다.
- Service/Application의 public 메서드는 하나의 유스케이스를 표현한다.
- Repository는 DB 접근 책임만 가진다.
- Domain은 상태와 규칙의 소유자다.
- DTO는 API 경계 모델이며 Entity와 분리한다.
- 조회 전용 복합 API는 `query` 패키지의 projection 모델을 우선 검토한다.

## DI

- 필수 의존성은 생성자 주입을 기본으로 한다.
- 필드는 가능한 `final`로 둔다.
- 필드 주입은 사용하지 않는다.
- 순환 참조가 생기면 구조 문제로 보고 설계를 다시 잡는다.

## DTO

- Request DTO에는 Bean Validation을 적극 사용한다.
- Request DTO를 Controller 경계 밖으로 무분별하게 흘리지 않는다.
- Response DTO는 Entity를 직접 노출하지 않는다.
- 날짜, 금액, enum 노출 방식은 API 계약에 맞춘다.
- 민감정보는 DTO에 필드를 만들기 전 권한과 마스킹 정책을 먼저 정한다.
- 평가위원 모드 DTO와 기업 PoC 모드 DTO를 분리한다.

## Entity와 Domain

- JPA Entity 기본 생성자는 `protected`로 둔다.
- Entity에 `@Data`를 쓰지 않는다.
- setter는 필요한 경우에만 제한적으로 제공한다.
- 상태 변경은 의미 있는 메서드로 표현한다.
- enum은 `EnumType.STRING`을 사용한다.
- 생성 규칙은 정적 팩토리나 생성자에 모은다.
- 도메인 불변조건을 Service에 흩뿌리지 않는다.
- 원천 오타 컬럼은 Entity에는 DB 호환명으로 매핑하되, 도메인/API 이름은 별도로 정리한다.

예:

```java
@Column(name = "employee_counteld3")
private Integer employeeCount;
```

## Repository와 Query

- Repository 메서드 이름은 조회 의도를 드러낸다.
- 긴 파생 쿼리는 `@Query`, Querydsl, Specification, QueryRepository를 검토한다.
- 목록/검색 API는 DTO projection 또는 query model을 우선 검토한다.
- N+1 가능성이 있는 API는 테스트나 SQL 로그로 확인한다.
- Repository에는 비즈니스 규칙을 넣지 않는다.
- pagination 없는 목록 API를 만들지 않는다.

## Transaction

- 트랜잭션은 public Service/Application 메서드의 유스케이스 단위에 둔다.
- 읽기 메서드는 `@Transactional(readOnly = true)`를 사용한다.
- 외부 API 호출을 DB 트랜잭션 안에 오래 붙잡아두지 않는다.
- private 메서드의 `@Transactional`은 프록시 기반 AOP에서 기대처럼 동작하지 않으므로 사용하지 않는다.
- 데이터 적재 작업은 chunk 단위 transaction을 검토한다.

## Migration

- Flyway 또는 Liquibase를 사용한다.
- migration은 코드와 같은 PR에서 관리한다.
- 첫 migration은 `docs/generated/db-schema.md`의 구현 대상만 포함한다.
- `company.industry_brief`, `company.ai_summary`, `company.ai_one_line_summary`, `Untitled4`는 만들지 않는다.
- DDL의 `VARHCAR`는 `VARCHAR`로 수정한다.
- 초기에는 데이터 품질 때문에 FK 제약을 유예할 수 있지만, 논리 관계와 인덱스는 문서와 코드에 반영한다.
- 금액/날짜/Boolean 변환 실패는 조용히 null 처리하지 않고 품질 리포트에 남긴다.

## Exception

- 비즈니스 예외와 시스템 예외를 구분한다.
- 예외 코드는 enum이나 별도 타입으로 관리한다.
- API 오류 응답 포맷은 `@RestControllerAdvice`에서 표준화한다.
- 사용자 메시지와 내부 로그 메시지를 구분한다.
- stack trace와 내부 상세를 API 응답에 노출하지 않는다.

## Validation

| 검증 종류 | 위치 |
|---|---|
| 형식 검증 | DTO + Bean Validation |
| 권한 검증 | Security 또는 Application |
| 상태 검증 | Domain |
| 중복/존재 검증 | Application 또는 Domain Service |
| 파일/컬럼 매핑 검증 | dataload quality |

## Logging

- 요청 식별자, 사용자 ID, 작업 ID 같은 추적 키를 남긴다.
- 민감정보는 로그에 남기지 않는다.
- 예외 로그는 stack trace를 보존한다.
- 정상 흐름 로그는 필요한 경우에만 `info`, 상세 흐름은 `debug`로 제한한다.
- 데이터 적재는 runId, sourceFileName, 실패 건수, 매핑 버전을 남긴다.

## Security

- 인증과 인가를 구분한다.
- URL 보안과 Method Security의 책임을 나눈다.
- CSRF, CORS, 세션, 토큰 정책을 명시적으로 설정한다.
- 권한 문자열 비교가 코드 곳곳에 흩어지지 않게 한다.
- 권한 검사는 테스트한다.
- 사업자등록번호, 주소, 기업명 원문 노출은 권한별 DTO에서 제어한다.

## API

- 리소스 중심 URL을 사용한다.
- 생성은 `201 Created`를 반환한다.
- 검증 실패는 `400`, 인증 실패는 `401`, 권한 없음은 `403`, 리소스 없음은 `404`를 사용한다.
- 파일 매핑/적재 검증 실패는 `422`를 검토한다.
- 오류 응답 포맷은 일관되게 유지한다.
- 공통 wrapper, pagination, enum 표현, 다국어 메시지 정책은 프로젝트 규칙으로 고정한다.

## Lombok

- 권장: `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Entity에 `@Data`를 사용하지 않는다.
- 양방향 연관 Entity에서 `@ToString`을 주의한다.
- `@EqualsAndHashCode`는 생명주기와 식별자 정책을 정한 뒤 사용한다.
- `@Builder`로 불완전한 도메인 객체가 만들어지지 않게 한다.

## 테스트

- 도메인 규칙은 순수 단위 테스트로 검증한다.
- Repository 쿼리는 `@DataJpaTest`로 검증한다.
- API 계약은 WebMvcTest 또는 통합 테스트로 검증한다.
- 모든 테스트를 `@SpringBootTest`로 만들지 않는다.
- 데이터 적재는 샘플 파일 기반 fixture와 실패 케이스를 함께 둔다.
- 목록 API는 pagination, sort, 권한별 필드 노출을 테스트한다.

## 안티패턴

- God Service
- Anemic Domain Model
- 무분별한 공통화
- `CommonUtils`, `DateUtils`, `StringUtils`가 계속 커지는 구조
- Controller에서 Repository 직접 호출
- 모든 예외를 HTTP 200 또는 `RuntimeException`으로 처리
- 모든 테스트에 `@SpringBootTest`
- Entity를 API로 직접 반환
- AI 요약/추천/판단 컬럼을 서비스 DB에 슬쩍 추가

## PR 체크리스트

- [ ] 정적 분석과 테스트가 통과하는가?
- [ ] 이름이 역할과 의도를 드러내는가?
- [ ] Controller가 얇은가?
- [ ] Service public 메서드가 하나의 유스케이스를 표현하는가?
- [ ] Entity를 API로 직접 노출하지 않는가?
- [ ] 트랜잭션 범위가 명확한가?
- [ ] 목록 API에 pagination이 있는가?
- [ ] N+1 가능성을 확인했는가?
- [ ] 오류 응답이 표준 포맷을 따르는가?
- [ ] 로그에 추적 정보가 있고 민감정보가 없는가?
- [ ] migration에 AI DB 제외 규칙을 어기지 않았는가?
- [ ] 평가위원 모드와 기업 PoC 모드의 공개 범위가 분리되어 있는가?
