# API 명세

> 개발 단계: **③ 설계** · 산출물: 엔드포인트, 요청·응답 형식
> 기준: `main` 코드 (`*ChainController.java`, `application*.yml`)

## 1. 엔드포인트

| 서비스 | 기본 포트 | 메서드 | 경로 | 설명 |
|---|---|---|---|---|
| gateway-service | 8080 (K8s NodePort 30080) | GET | `/api/chain` | 부하 테스트 진입점. Order를 호출 |
| order-service | 8081 | GET | `/api/internal/chain` | Inventory를 호출 |
| inventory-service | 8082 | GET | `/api/internal/chain` | User를 호출 |
| user-service | 8083 | GET | `/api/internal/chain` | 체인 끝. 다른 서비스를 부르지 않음 |
| 모든 서비스 | 위와 같음 | GET | `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` | 상태 확인, 메트릭 |

## 2. 요청

| 항목 | `no-auth` | `jwt-relay` |
|---|---|---|
| 헤더 | 없음 | `Authorization: Bearer <JWT>` |
| 본문 | 없음 | 없음 |
| 토큰 | — | HS256, 서비스와 같은 `JWT_SECRET`으로 서명. `benchmark-tools`로 생성 ([README](../README.md#빠른-시작)) |

`jwt-relay`에서는 **actuator 경로를 포함한 모든 요청**에 토큰이 필요합니다([VERIFICATION.md §3](./VERIFICATION.md#3-알려진-문제)).

## 3. 응답

각 서비스가 아래 응답을 받아 `downstream`에 넣고 자기 정보를 덧붙입니다. 그래서 Gateway 응답 하나에 체인 전체가 중첩되어 담깁니다.

```json
{
  "service": "gateway",
  "depth": 0,
  "mode": "no-auth",
  "downstream": {
    "service": "order",
    "depth": 1,
    "mode": "no-auth",
    "downstream": {
      "service": "inventory",
      "depth": 2,
      "mode": "no-auth",
      "downstream": { "service": "user", "depth": 3, "mode": "no-auth" }
    }
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `service` | string | 응답한 서비스 |
| `depth` | number | 체인 위치. Gateway 0 ~ User 3 |
| `mode` | string | 그 서비스의 `APP_MODE`. 조건 설정이 서비스마다 같은지 확인하는 용도 |
| `downstream` | object | 다음 서비스의 응답. User에는 없음 |

## 4. 상태 코드

| 코드 | 언제 |
|---|---|
| 200 | 체인 전체 성공 |
| 401 | `jwt-relay`에서 토큰이 없거나, 서명·만료 검증 실패 |
| 500 | 하위 서비스 호출 실패(연결 거부, 타임아웃, 하위 서비스 401 등). Feign 예외가 그대로 올라옴 |

## 5. 서비스 간 호출 설정

| 호출 | Feign 클라이언트 | 대상 주소 환경변수 | K8s 값 |
|---|---|---|---|
| Gateway → Order | `OrderServiceClient` | `ORDER_SERVICE_URL` | `http://order-service:8081` |
| Order → Inventory | `InventoryServiceClient` | `INVENTORY_SERVICE_URL` | `http://inventory-service:8082` |
| Inventory → User | `UserServiceClient` | `USER_SERVICE_URL` | `http://user-service:8083` |

서비스 간 호출은 HTTP 평문입니다. Step 3의 암호화는 애플리케이션이 아니라 Envoy 사이드카가 합니다.
