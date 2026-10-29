# 실험 런북

> 개발 단계: **⑦ 수정 및 문서화** · 산출물: 장애 유형별 대응 절차
> 형식: 증상 → 영향 → 확인 → 조치 → 재발 방지 → 기록. 로컬 Docker 환경 문제는 [실험 사용법 가이드 11단계](./실험-사용법-가이드.md)를 보세요.

| ID | 상황 |
|---|---|
| [RB-01](#rb-01-jaeger-메모리-증가로-노드가-멈춤) | Jaeger 메모리 증가로 노드가 멈춤 |
| [RB-02](#rb-02-step-1에서-pod가-ready가-되지-않음) | Step 1에서 Pod가 Ready가 되지 않음 |
| [RB-03](#rb-03-pod가-pending에서-멈춤) | Pod가 Pending에서 멈춤 |
| [RB-04](#rb-04-jwt-요청이-401-또는-500) | JWT 요청이 401 또는 500 |
| [RB-05](#rb-05-사이드카가-주입되지-않거나-mtls-설정이-기대와-다름) | 사이드카·mTLS 설정이 기대와 다름 |
| [RB-06](#rb-06-같은-조건인데-회차마다-결과가-크게-다름) | 같은 조건인데 회차마다 결과가 크게 다름 |

---

## RB-01 Jaeger 메모리 증가로 노드가 멈춤

| 항목 | 내용 |
|---|---|
| 증상 | 고부하 측정 중 Jaeger 메모리가 계속 늘다가, 노드가 응답하지 않거나 Pod가 OOMKilled 됨 |
| 영향 | 측정 중단. 같은 노드의 다른 컴포넌트도 영향을 받아 해당 회차 데이터는 버려야 함 |
| 확인 | `kubectl top pod -A`로 Jaeger 메모리 추이, `kubectl describe pod`의 `OOMKilled`, 노드 `free -h` |
| 조치 | 1) 측정 중단, Jaeger 재시작 2) `TRACING_SAMPLE_PROBABILITY`를 낮춤(ConfigMap 수정 후 `rollout restart`) 3) Jaeger all-in-one이면 메모리 저장 한도(`MEMORY_MAX_TRACES`)를 두거나 외부 저장소 사용 4) Jaeger에 memory limit 지정 |
| 재발 방지 | 본 측정 전에 고부하로 5분 이상 돌려 관측 도구 메모리가 평탄해지는지 확인. 샘플링 비율을 조건별로 같게 고정하고 기록 |
| 기록 | 날짜, 조건·부하, 샘플링 비율, 바꾼 설정 |

배운 점: **관측 도구 설정도 측정 결과를 바꿉니다.** 샘플링 비율도 인증 방식처럼 조건마다 고정하고 기록합니다.

## RB-02 Step 1에서 Pod가 Ready가 되지 않음

| 항목 | 내용 |
|---|---|
| 증상 | `APP_MODE=jwt-relay`로 바꾼 뒤 Pod가 `0/1 Ready`이거나 계속 재시작 |
| 영향 | Step 1 측정 불가 |
| 확인 | `kubectl -n msa-benchmark describe pod <pod>`에서 프로브 실패 `HTTP probe failed with statuscode: 401` |
| 조치 | [VERIFICATION.md KI-01](./VERIFICATION.md#ki-01-jwt-relay-모드에서-actuator가-401을-반환)의 해결 방법 적용 (actuator 인증 제외 또는 tcpSocket 프로브) |
| 재발 방지 | 조건을 바꾼 뒤 `get pods`로 Ready를 확인하고 나서 Startup Wait 시작 |
| 기록 | 적용한 해결 방법 |

## RB-03 Pod가 Pending에서 멈춤

| 항목 | 내용 |
|---|---|
| 증상 | Pod가 `Pending`, 이벤트에 `didn't match Pod's node affinity` |
| 영향 | 배포 불가 |
| 확인 | `kubectl get nodes`의 이름과 매니페스트 `nodeAffinity` 값(`node3`) 비교 |
| 조치 | 4개 매니페스트의 `node3`을 실제 워커 노드 이름으로 바꾸고 다시 `apply` |
| 재발 방지 | 새 클러스터에서는 배포 전에 노드 이름부터 맞춤 |
| 기록 | 사용한 노드 이름 |

`ImagePullBackOff`라면 워커 노드에 이미지가 없는 것입니다. [DEPLOYMENT.md §2](./DEPLOYMENT.md#2-이미지-준비)대로 이미지를 옮깁니다.

## RB-04 JWT 요청이 401 또는 500

| 항목 | 내용 |
|---|---|
| 증상 | `/api/chain`이 401, 또는 Gateway는 통과했는데 500 |
| 영향 | Step 1 측정값 오염(오류 응답이 TPS에 섞임) |
| 확인 | 1) 헤더가 `Authorization: Bearer <토큰>` 형식인지 2) 토큰 만료(기본 60분) 3) 모든 서비스의 `JWT_SECRET`이 같은지 4) 500이면 하위 서비스 로그에서 401·연결 오류 확인 |
| 조치 | 토큰 재발급(`--ttl-minutes`로 측정 시간보다 길게), ConfigMap 값 통일 후 재시작 |
| 재발 방지 | 측정 직전에 토큰을 새로 만들고 curl로 200 확인. JMeter 결과에서 오류율이 0%인지 확인 |
| 기록 | 토큰 발급 시각, TTL, 오류율 |

## RB-05 사이드카가 주입되지 않거나 mTLS 설정이 기대와 다름

| 항목 | 내용 |
|---|---|
| 증상 | Step 2·3인데 Pod가 `1/1`(사이드카 없음), 또는 Step 2에서 트래픽이 mTLS로 암호화됨 |
| 영향 | 조건이 섞여 프록시 비용·mTLS 비용 분리가 틀어짐 |
| 확인 | `kubectl get ns msa-benchmark --show-labels`, Pod READY 수(`2/2`), `istioctl x describe pod`, `kubectl get peerauthentication,destinationrule -n msa-benchmark` |
| 조치 | 네임스페이스 라벨 후 `rollout restart`. Step 2는 mTLS `DISABLE`, Step 3은 `STRICT` 적용([DEPLOYMENT.md §4](./DEPLOYMENT.md#4-조건별-설정)) |
| 재발 방지 | 조건마다 위 확인 명령 결과를 측정 기록에 함께 남김 |
| 기록 | 조건별 Istio 리소스 YAML |

## RB-06 같은 조건인데 회차마다 결과가 크게 다름

| 항목 | 내용 |
|---|---|
| 증상 | 같은 조건 3회의 TPS·지연이 크게 다름 |
| 영향 | 대표 회차 선정이 의미 없어짐 |
| 확인 | Startup Wait·Warm-up을 지켰는지, 워커 노드에 다른 Pod가 떴는지(`kubectl get pods -A -o wide`), JMeter 오류율, 관측 도구 상태 |
| 조치 | 원인을 없앤 뒤 해당 조건 3회를 다시 측정 |
| 재발 방지 | 회차 사이에도 Startup Wait를 지킴. 측정 중에는 클러스터에 다른 작업을 하지 않음 |
| 기록 | 회차별 원자료와 제외 사유 |
