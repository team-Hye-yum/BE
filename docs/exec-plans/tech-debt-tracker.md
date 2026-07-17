# Tech Debt Tracker

기술 부채는 발견 즉시 기록하고, 정기적으로 작은 PR로 갚습니다.

| ID | 영역 | 증상 | 위험 | 처리 방향 | 상태 |
|---|---|---|---|---|---|
| TD-001 | 예시 | God Service 증가 | 변경 영향 범위 예측 어려움 | 유스케이스별 Service 분리 | 대기 |

## 자주 기록할 부채

- God Service
- Anemic Domain Model
- 무분별한 공통 util
- Entity 직접 응답
- N+1 가능성
- 표준화되지 않은 예외 응답
- `@SpringBootTest` 남발
- 외부 API timeout 누락
- 민감 정보 로그 가능성
