# Observability Reference

## Log keys

- `requestId`
- `userId`
- domain IDs such as `orderId`, `memberId`, `paymentId`
- external system name
- result status

## Never log

- password
- access token
- refresh token
- API key
- raw personal identification data
- payment secret

## Error logging

예외 객체를 마지막 인자로 전달해 스택트레이스를 보존합니다.

```java
log.error("Payment approval failed. orderId={}", orderId, exception);
```
