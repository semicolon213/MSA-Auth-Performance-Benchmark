# 코드 검증

> 개발 단계: **⑥ 테스트** · 산출물: 빌드·동작 확인 결과, 알려진 문제
> 실험 결과(성능 측정)는 [RESULTS.md](./RESULTS.md)에 있습니다. 이 문서는 **코드가 의도대로 동작하는지**를 확인한 기록입니다.

## 1. 확인 방법

자동 테스트 코드는 없습니다(각 서비스에 `spring-boot-starter-test` 의존성만 있음). 아래 항목을 직접 실행해 확인했습니다.

| 날짜 | 2026-10-27 |
|---|---|
| 코드 | `main` @ `b2272ca` |
| 환경 | Docker `maven:3.9-eclipse-temurin-17` 컨테이너 |

## 2. 결과

| ID | 확인 항목 | 방법 | 기대 | 결과 |
|---|---|---|---|---|
| V-01 | 전체 빌드 | `mvn -B -DskipTests package` | 모듈 8개 성공 | ✅ BUILD SUCCESS (약 27초) |
| V-02 | `no-auth`에서 헬스 체크 | user-service 실행 후 `GET /actuator/health` | 200 | ✅ 200 |
| V-03 | `no-auth`에서 체인 호출 | `GET /api/internal/chain` | 200 | ✅ 200 |
| V-04 | `jwt-relay`에서 토큰 없이 호출 | 같은 요청, 헤더 없음 | 401 | ✅ 401 |
| V-05 | `jwt-relay`에서 토큰으로 호출 | `benchmark-tools`로 만든 토큰 + `Authorization: Bearer` | 200, `"mode":"jwt-relay"` | ✅ `{"service":"user","depth":3,"mode":"jwt-relay"}` |
| V-06 | `jwt-relay`에서 헬스 체크 | `GET /actuator/health` | 200 (프로브용) | ❌ **401** → [§3 KI-01](#ki-01-jwt-relay-모드에서-actuator가-401을-반환) |

체인 전체(Gateway부터)는 Docker Compose로 확인합니다([실험 사용법 가이드 3·7단계](./실험-사용법-가이드.md)).

## 3. 알려진 문제

### KI-01 `jwt-relay` 모드에서 actuator가 401을 반환

- **원인:** `JwtRelaySecurityConfig`가 `anyRequest().authenticated()`로 모든 경로에 토큰을 요구합니다. `/actuator/health`, `/actuator/prometheus`도 포함됩니다.
- **영향:**
  - `k8s/*-service.yaml`의 liveness·readiness 프로브가 401로 실패해, Step 1에서 Pod가 Ready가 되지 않거나 재시작됩니다.
  - Prometheus가 앱 메트릭(`/actuator/prometheus`)을 읽지 못합니다. 컨테이너 CPU·메모리(kubelet/cAdvisor)는 영향이 없습니다.
- **해결 방법 (택1):**
  1. 코드: actuator 경로를 인증에서 뺍니다.
     ```java
     .authorizeHttpRequests(a -> a
         .requestMatchers("/actuator/**").permitAll()
         .anyRequest().authenticated())
     ```
  2. 매니페스트: 프로브를 `tcpSocket`으로 바꿉니다.
- 1번은 Step 1 이미지의 보안 필터 체인을 바꾸므로, 측정 전에 적용하고 결과 문서에 적습니다.

### KI-02 Istio·관측 도구 설정이 저장소에 없음

Step 2·3의 `PeerAuthentication`·`DestinationRule`, Jaeger·Prometheus 설치 설정이 저장소에 없습니다. 같은 조건을 다시 만들려면 [DEPLOYMENT.md §4](./DEPLOYMENT.md#4-조건별-설정)의 예시를 쓰고, 실제 사용한 파일을 `k8s/istio/` 등에 보관해야 합니다.

### KI-03 사이드카 주입 안내가 user-service에만 있음

`sidecar.istio.io/inject` 주석은 `k8s/user-service.yaml`에만 있습니다. 네임스페이스 라벨(`istio-injection=enabled`)로 주입하면 매니페스트를 고칠 필요는 없습니다.

### KI-04 JMeter 플랜 기본값이 실험 조건과 다름

`jmeter/*.jmx`는 10스레드, Ramp-up 30초, 300초 지속입니다. 실험 조건(30/300/800 스레드, 60초 측정)은 GUI나 `-J` 속성으로 바꿔야 합니다([EXPERIMENT_DESIGN.md §3](./EXPERIMENT_DESIGN.md#3-부하-케이스)).

### KI-05 Dockerfile `EXPOSE 8080` 고정

모든 서비스 이미지가 `EXPOSE 8080`으로 표시됩니다. 실제 포트는 `SERVER_PORT`로 정해져 동작에는 영향이 없지만, 이미지 정보와 다릅니다.
