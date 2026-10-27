# MSA Auth Performance Benchmark

> **마이크로서비스(MSA) 환경의 제로 트러스트 서비스 간 통신 인증 방식 성능 분석** — 학사 학위 논문 프로젝트  
> **JWT Token Relay**(애플리케이션 계층) vs **Istio mTLS**(인프라 계층)

| 항목 | 내용 |
|---|---|
| 기간 | 2026.03 ~ 진행 중 |
| 인원 | 2명 |
| 형태 | 학사 학위 논문 (유한대학교 컴퓨터공학과) |

**처음부터 실험까지 따라 하기 (Ubuntu 기준):** [실험 사용법 가이드 (단계별 상세)](docs/실험-사용법-가이드.md)

## 프로젝트 소개
제로 트러스트 아키텍처(ZTA)는 "내부의 어떤 주체도 신뢰하지 않고 항상 검증한다"는 원칙에 따라 **서비스 간 내부 통신(East-West)에도 암호화와 상호 인증**을 요구합니다.
이를 구현하는 대표적인 방법은 두 가지입니다.

- **JWT Token Relay** — 애플리케이션 계층에서 각 서비스가 토큰 서명을 직접 검증하고 다음 서비스로 전달
- **Istio mTLS** — 인프라 계층에서 Envoy 사이드카 프록시가 상호 인증서 교환과 암호화를 대신 처리

이 프로젝트는 Kubernetes 위에 **API Gateway → Order → Inventory → User**의 3-Depth 연쇄 호출 환경을 구축하고,
같은 애플리케이션 코드에서 인증 방식만 바꿔 가며 **처리량(TPS), 종단 간 지연 시간, 컨테이너별 CPU 사용량**을 비교했습니다.

### 연구 질문
기존 연구는 대부분 단일 홉 통신이나 개별 기술의 성능만 다뤘습니다. 이 프로젝트는 다음 공백을 실험으로 검증합니다.
- 실제 서비스와 비슷한 **3-Depth 이상 연쇄 호출**에서 JWT Relay와 mTLS의 성능은 어떻게 다른가?
- 서비스 메시의 비용을 **사이드카 프록시 경유 비용**과 **순수 mTLS 암복호화 비용**으로 분리하면 각각 얼마인가?
- 인증 연산은 **어느 컨테이너의 자원**을 소모하며, 호출 깊이에 따라 어떻게 누적되는가?

## 실험 환경
VMware Workstation Pro 기반 가상머신 3대를 **부하 발생 / 제어·모니터링 / 워커** 역할로 분리해, 측정 대상과 부하 발생기·관측 도구 사이의 자원 간섭을 막았습니다.

| 노드 | CPU | Memory | OS | 역할 |
|---|---|---|---|---|
| Master Node | 6 Core | 12GB | Ubuntu 22.04 | Kubernetes Control Plane, Jaeger·Prometheus 수집 |
| Worker Node | 6 Core | 12GB | Ubuntu 22.04 | 마이크로서비스 4개 실행 |
| Load Generator | 6 Core | 12GB | Ubuntu 22.04 | JMeter 부하 발생 |

- **Platform:** Kubernetes v1.28.15, Istio
- **Application:** Java 17, Spring Boot 3.2.5, OpenFeign

## 실험 설계
애플리케이션 코드는 그대로 두고 **`APP_MODE`와 인프라 구성만 바꿔** 조건을 통제했습니다.

| 단계 | 명칭 | 보안 레이어 위치 | APP_MODE | 설명 |
|---|---|---|---|---|
| Step 0 | Baseline | 인증 없음 | `no-auth` | 사이드카 없는 순수 네트워크 성능 기준값 |
| Step 1 | JWT Relay | 애플리케이션 계층 | `jwt-relay` | Spring Security로 JWT 서명 검증, 사이드카 미주입 |
| Step 2 | Sidecar Only | 인프라 계층 (프록시) | `no-auth` | Envoy 사이드카 주입, mTLS 비활성화 → 순수 프록시 비용 |
| Step 3 | Istio mTLS | 인프라 계층 (인증) | `no-auth` | Istio **STRICT** 모드로 상호 TLS 암복호화·인증 적용 |

### 부하 테스트 케이스
부하 강도는 Step 0 환경의 CPU 사용률을 기준으로 80~85%, 85~90%, 90% 이상이 되도록 정했습니다.

| 구분 | 케이스 | Threads | Ramp-up | 측정 시간 |
|---|---|---|---|---|
| 저부하 | TC 1 | 30 | 3s | 60s |
| 중부하 | TC 2 | 300 | 30s | 60s |
| 고부하 | TC 3 | 800 | 80s | 60s |

### 측정 절차
```text
[Pod 배포 완료] → Startup Wait (1~3분) → Warm-up (10~60초) → Main Test (60초 본 측정)
```
- **Startup Wait:** 프레임워크 초기화, 커넥션 풀 생성, 컨트롤 플레인 CPU 스파이크가 측정에 섞이지 않도록 대기
- **Warm-up:** JVM JIT 컴파일과 클래스 로딩으로 인한 콜드 스타트 지연을 측정 전에 제거
- **반복 측정:** 조건마다 3회 측정하고, 평균에 가장 가까운 회차를 대표 케이스로 사용

### 평가 지표
- **처리량·지연 시간:** TPS, 평균 응답 시간 (p99 보조 지표)
- **자원 사용량:** 워커 노드의 컨테이너별 평균 CPU·메모리
- **구간 지연:** Jaeger 스팬으로 네트워크 라우팅 시간과 인증·암복호화 시간을 분리

## 실험 결과
### 처리량 및 지연 시간
| 지표 | 부하 | Baseline | JWT Token Relay | Istio mTLS |
|---|---|---|---|---|
| 최대 처리량 (TPS) | 800 | 3,000대 | 1,700대 | 1,600~1,700 |
| 평균 지연 시간 | 30 | 약 12ms | 17~18ms | 17~18ms |
| | 300 | 약 98ms | 160~170ms | 약 168ms |
| | 800 | 약 257ms | 430~440ms | 약 480ms |

- 저부하에서는 JWT와 mTLS의 지연 시간 차이가 거의 없었습니다.
- 고부하(800명)에서는 **JWT도 1,700 TPS대까지 떨어져 mTLS와 처리량이 비슷**해졌습니다. 반복되는 서명 검증 연산이 프록시 오버헤드와 비슷한 수준으로 처리량을 낮춘다는 뜻입니다.

### 순수 mTLS 오버헤드 (Step 2 vs Step 3)
| 부하 | Sidecar Only | Istio mTLS | 순수 mTLS 오버헤드 |
|---|---|---|---|
| 저부하 (30) | 유사 | 유사 | 차이 없음 |
| 중부하 (300) | 165ms | 168ms | **약 3ms** |
| 고부하 (800) | 458ms | 479ms | **약 21ms** |

고부하에서도 순수 암복호화 비용은 약 21ms 수준이었습니다. TLS 핸드셰이크 비용이 HTTP 커넥션 풀링으로 상쇄되기 때문입니다.

### 컨테이너별 CPU 사용량 (고부하, 애플리케이션 컨테이너 기준)
| 서비스 | JWT Token Relay | Istio mTLS |
|---|---|---|
| gateway-service | 1,986.37m | **521.7m** (+ istio-proxy 786.18m) |
| order-service | 2,115.88m | 1,659.1m |
| inventory-service | 1,905.72m | 1,487.67m |
| user-service | 1,209.37m | 473.48m |

- **JWT:** 모든 서비스가 매 호출마다 서명을 검증하므로, 호출 깊이를 따라 **애플리케이션 CPU가 연쇄적으로 고점유** 상태가 되었습니다.
- **mTLS:** 암복호화를 Envoy 사이드카가 맡아 **애플리케이션 CPU가 크게 줄었고**, 보안 연산 부하가 프록시 계층으로 격리되었습니다.

### 결론
mTLS는 고부하에서 약 21ms의 지연을 더하지만, 처리량은 JWT와 비슷했습니다. 동시에 보안 연산을 애플리케이션 밖으로 빼내 비즈니스 로직의 자원을 보호하고, 여러 언어로 된 서비스에도 같은 정책을 일괄 적용할 수 있습니다.
따라서 **대규모 MSA의 제로 트러스트 구현에는 Istio mTLS가 더 적합하다**고 결론지었습니다.

## 아키텍처
```text
┌─ Load Generator ─┐   ┌──────────────── Worker Node (Kubernetes) ────────────────┐
│     JMeter       │──▶│ Gateway ──▶ Order ──▶ Inventory ──▶ User                 │
└──────────────────┘   │  :8080 Feign :8081 Feign :8082  Feign  :8083             │
                       │  (Step 2·3: 각 Pod에 Envoy 사이드카)                     │
                       └───────────────────────────┬──────────────────────────────┘
                                                   │ OTLP 트레이스 · 메트릭 수집
                       ┌─ Master Node ─────────────▼──────────────────┐
                       │ Kubernetes Control Plane, Jaeger, Prometheus │
                       └──────────────────────────────────────────────┘

[Step 1] Authorization: Bearer <JWT> 를 각 서비스가 검증하고 Feign으로 릴레이
[Step 3] 서비스 간 트래픽을 Istio 사이드카(Envoy)가 mTLS STRICT로 암호화·인증
```

## 기술 스택
| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5, Spring Cloud 2023.0.1 (OpenFeign) |
| Security | Spring Security OAuth2 Resource Server (JWT HS256), JJWT |
| Service Mesh | Istio (Envoy 사이드카, mTLS STRICT) |
| Infra | Kubernetes v1.28.15, Docker, Docker Compose, VMware Workstation Pro |
| Observability | Micrometer Tracing, OpenTelemetry OTLP, Jaeger, Prometheus |
| Load Test | Apache JMeter |
| Build | Maven (멀티 모듈) |

## 구현 포인트
- **단일 코드베이스, 프로파일로 조건 전환** — `APP_MODE`가 Spring 프로파일과 연동됩니다. `no-auth`에서는 Security 자동 구성을 제외하고, `jwt-relay`에서만 `@ConditionalOnProperty`로 보안 설정이 활성화됩니다. 실험 조건 사이에 코드 차이가 생기지 않습니다.
- **JWT 릴레이** — 각 서비스가 Resource Server로 HS256 서명을 검증하고, Feign `RequestInterceptor`가 들어온 `Authorization` 헤더를 다음 서비스로 그대로 전달합니다.
- **공통 모듈 분리** — 보안 설정(`benchmark-common`)과 Feign 인터셉터(`benchmark-feign-common`)를 모듈로 분리해 4개 서비스가 같은 구현을 공유합니다.
- **관측 가능성** — 모든 서비스가 OTLP로 Jaeger에 트레이스를 보내고, 로그에 traceId/spanId를 남겨 구간별 지연을 추적할 수 있습니다.
- **측정 환경 분리** — 부하 발생기, 컨트롤 플레인·관측 도구(Jaeger·Prometheus), 측정 대상 서비스를 서로 다른 노드에 배치하고, 서비스는 node affinity로 워커 노드에 고정해 자원 간섭을 줄였습니다.

## 트러블슈팅
### Jaeger 메모리 증가로 인한 노드 중단
- **문제:** 부하 테스트 중 Jaeger가 트레이스를 메모리에 계속 쌓으면서 메모리 사용량이 늘어났고, 결국 노드가 중단되었습니다.
- **해결:** 트레이싱 설정(샘플링 비율 `TRACING_SAMPLE_PROBABILITY` 등)을 조정해 측정 환경을 안정화했습니다.
- **배운 점:** 인증 방식뿐 아니라 **관측 도구의 설정도 측정 결과에 영향을 준다**는 점을 확인했습니다. 성능 실험에서는 측정 도구의 부하도 통제 변수로 다뤄야 합니다.

## 팀원 및 역할
| 팀원 | 역할 | 담당 기능 |
|---|---|---|
| 김형균 | PM | 프로젝트 설계, Kubernetes 워커 노드 및 실험 환경 구축, JWT Relay·Istio mTLS 비교 환경 구성, 성능 측정·분석, 논문 작성 |
| 최가은 | 팀원 | 구현 세부 오류 수정, 노드 배포 설정, 부하 테스트 및 성능 측정 |

## 관련 문서
- [논문 계획서](https://docs.google.com/document/d/1RiJlxUNSNpEuJ8rgcCDKBGPZNdBSaH8e/edit?usp=sharing&ouid=104391096948093738115&rtpof=true&sd=true)
- [논문 본문](https://docs.google.com/document/d/1y2XVFpZfJl1_UD54xl5DnxSDcFfAZuR2/edit?usp=sharing&ouid=104391096948093738115&rtpof=true&sd=true)

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
├── benchmark-tools/                 # HS256 JWT 생성 CLI (JJWT)
├── jmeter/                          # JMeter 샘플 JMX
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
