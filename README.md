# MSA Auth Performance Benchmark

마이크로서비스(MSA) 환경에서 **JWT Token Relay**(애플리케이션 계층)와 **Istio mTLS**(인프라 계층) 성능 비교 실험을 위한 **4단계 체인** 데모 애플리케이션입니다.

**처음부터 실험까지 따라 하기 (Ubuntu 기준):** [실험 사용법 가이드 (단계별 상세)](docs/실험-사용법-가이드.md)

**호출 체인:** API Gateway → Order → Inventory → User (3-Depth 내부 호출 + Gateway)

---

## 기술 스택

| 항목 | 버전/구성 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Cloud | 2023.0.1 (OpenFeign) |
| 보안 | Spring Security OAuth2 Resource Server (JWT HS256, `jwt-relay` 모드만) |
| 관측 | Micrometer Tracing + OpenTelemetry OTLP → Jaeger |
| 메트릭 | Actuator, Prometheus Registry |

---

## 프로젝트 구조

```
msa-auth-performance-benchmark/
├── pom.xml                          # 부모 POM
├── Dockerfile                       # 모듈별 이미지 (--build-arg SVC=...)
├── docker-compose.yml               # 로컬 통합 실행 + Jaeger
├── .env.example                     # 환경 변수 템플릿 (복사 → .env)
├── .gitignore
├── .dockerignore
├── benchmark-common/                # AppProperties, JwtRelaySecurityConfig
├── benchmark-feign-common/          # Feign Authorization 릴레이 인터셉터
├── gateway-service/                 # 진입점 (기본 8080)
├── order-service/                   # 8081
├── inventory-service/               # 8082
├── user-service/                    # 8083 (리프)
├── benchmark-tools/               # HS256 JWT 생성 CLI (JJWT)
├── jmeter/                        # JMeter 샘플 JMX
│   ├── msa-chain-no-auth.jmx
│   ├── msa-chain-jwt.jmx
│   └── README.md
└── k8s/                             # Kubernetes 매니페스트
    ├── namespace.yaml
    ├── configmap-env.yaml
    ├── user-service.yaml
    ├── inventory-service.yaml
    ├── order-service.yaml
    └── gateway-service.yaml
```

---

## APP_MODE와 실험 단계 매핑

환경 변수 **`APP_MODE`**는 Spring **`spring.profiles.active`**와 동일하게 동작합니다.

| APP_MODE | 프로파일 파일 | 동작 |
|----------|----------------|------|
| `no-auth` | `application-no-auth.yml` | Spring Security·OAuth2 Resource Server **자동구성 제외**. 앱 내부 인증 없음. **Step 0(베이스라인), Step 2(사이드카), Step 3(mTLS)** 공통 코드. |
| `jwt-relay` | `application-jwt-relay.yml` | JWT 서명 검증 + Feign으로 `Authorization` **릴레이**. **Step 1**. |

---

## 주요 환경 변수

| 변수 | 설명 | 기본 예시 |
|------|------|-----------|
| `APP_MODE` | `no-auth` 또는 `jwt-relay` | `no-auth` |
| `SERVER_PORT` | 서비스 HTTP 포트 | 서비스별 상이 |
| `ARTIFICIAL_LATENCY_MS` | 각 체인 컨트롤러에서 `Thread.sleep` (ms), 병목 모사 | `0` |
| `JWT_SECRET` | HS256 검증용 공유 비밀 (모든 서비스 동일) | `application.yml` 참고 |
| `OTLP_TRACES_ENDPOINT` | OTLP HTTP 트레이스 수집 URL | `http://localhost:4318/v1/traces` |
| `TRACING_SAMPLE_PROBABILITY` | 트레이스 샘플링 비율 | `1.0` |

**서비스 URL (다운스트림 Feign):**

| 변수 | 대상 |
|------|------|
| `ORDER_SERVICE_URL` | Gateway → Order |
| `INVENTORY_SERVICE_URL` | Order → Inventory |
| `USER_SERVICE_URL` | Inventory → User |

---

## API 엔드포인트

| 서비스 | 메서드 | 경로 | 설명 |
|--------|--------|------|------|
| Gateway | GET | `/api/chain` | 부하 테스트·E2E 진입점 |
| Order / Inventory / User | GET | `/api/internal/chain` | 내부 연쇄 호출용 |
| 공통 | GET | `/actuator/health` | 헬스 체크 |
| 공통 | GET | `/actuator/prometheus` | Prometheus 메트릭 |

---

## 로컬 빌드 (Maven)

JDK 17, Maven 3.9+ 필요.

```bash
mvn -DskipTests package
```

---

## Docker 이미지 빌드

저장소 **루트**에서 `SVC`에 모듈명 지정:

```bash
docker build --build-arg SVC=gateway-service -t msa-gateway-service:latest .
docker build --build-arg SVC=order-service -t msa-order-service:latest .
docker build --build-arg SVC=inventory-service -t msa-inventory-service:latest .
docker build --build-arg SVC=user-service -t msa-user-service:latest .
```

---

## Docker Compose

Jaeger UI: `http://localhost:16686`  
OTLP HTTP: 컨테이너 `jaeger:4318`

프로젝트 루트에 **`.env`** 파일이 있으면 Docker Compose가 자동으로 읽습니다. 템플릿은 **`.env.example`** 을 복사해 만듭니다.

```bash
cp .env.example .env
# 필요 시 .env 를 편집한 뒤
docker compose up --build
```

`.env` 는 비밀키가 들어갈 수 있어 **Git에 올리지 않습니다** (`.gitignore` 처리).

기본은 `APP_MODE=no-auth`. JWT 실험 시 `.env` 에서 `APP_MODE=jwt-relay` 로 바꾸거나, 일회성으로:

```bash
export APP_MODE=jwt-relay
docker compose up --build
```

부하/수동 호출 진입 URL: **`http://localhost:8080/api/chain`**

---

## JWT-Relay 모드에서 호출 방법

모든 서비스에 **동일한 `JWT_SECRET`**으로 서명된 **HS256** JWT를 사용합니다.  
Gateway 요청에 다음 헤더를 포함합니다.

```http
Authorization: Bearer <JWT>
```

Feign이 Order → Inventory → User로 `Authorization` 헤더를 전달합니다.

---

## HS256 JWT 생성 (`benchmark-tools`)

애플리케이션 기본 `jwt.secret`과 동일한 HS256 토큰을 표준 출력으로 생성합니다.

```bash
mvn -pl benchmark-tools -am -DskipTests package
java -jar benchmark-tools/target/benchmark-tools-1.0.0-SNAPSHOT-exec.jar --secret "benchmark-jwt-hmac-secret-key-min-32-bytes!!"
```

또는:

```bash
mvn -pl benchmark-tools exec:java -Dexec.args="--secret benchmark-jwt-hmac-secret-key-min-32-bytes!!"
```

옵션: `--subject`, `--issuer`, `--ttl-minutes`, `--help`. 비밀키는 **최소 32바이트**여야 합니다.

---

## JMeter 샘플

`jmeter/` 디렉터리에 **no-auth** / **jwt-relay**용 테스트 플랜이 있습니다. 스레드·램프업·5분 스케줄 등은 논문 계획에 맞게 GUI에서 조정할 수 있습니다.

상세 사용법은 [`jmeter/README.md`](jmeter/README.md)를 참고하세요.

---

## Kubernetes

1. 이미지를 레지스트리에 푸시하거나, 노드에 로드한 뒤 `imagePullPolicy`에 맞게 조정합니다.
2. `k8s/configmap-env.yaml`에서 `APP_MODE`, `OTLP_TRACES_ENDPOINT`, `JWT_SECRET` 등을 클러스터 환경에 맞게 수정합니다.
3. **노드 어피니티**는 `kubernetes.io/hostname: node3`으로 고정되어 있습니다. 실제 세 번째 워커 호스트명으로 **반드시 교체**하세요.

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap-env.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/inventory-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/gateway-service.yaml
```

Gateway 서비스는 **NodePort 30080**으로 노출됩니다 (매니페스트 기준).

**Istio 사이드카 주입:** 각 Deployment의 `spec.template.metadata.labels`에 주석으로 안내해 두었습니다. 클러스터 정책에 맞게 `sidecar.istio.io/inject: "true"` 등을 활성화하면 됩니다.

---

## 핵심 소스 위치

| 구분 | 경로 |
|------|------|
| JWT Resource Server 설정 | `benchmark-common/.../JwtRelaySecurityConfig.java` |
| Feign JWT 릴레이 | `benchmark-feign-common/.../FeignAuthorizationRelayInterceptor.java` |
| Feign 클라이언트 | `gateway-service/.../client/`, `order-service/.../client/`, `inventory-service/.../client/` |
| 체인 컨트롤러 | 각 서비스 `.../web/*ChainController.java` |

---

## 라이선스

저장소 루트의 `LICENSE` 파일을 따릅니다.
