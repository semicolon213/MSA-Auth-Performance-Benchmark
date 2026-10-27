# Kubernetes 실험 환경 배포

> 개발 단계: **④ 구조 생성** · 산출물: 인프라 구성, 조건별 배포 절차
> 로컬(Docker Compose) 실행은 [실험 사용법 가이드](./실험-사용법-가이드.md)를 보세요.

> **알아 둘 점:** 이 저장소에는 서비스 매니페스트(`k8s/`)만 있습니다. Kubernetes 클러스터 구성, Istio 설치·정책, Jaeger·Prometheus 설치 파일은 없습니다. 아래 §4~§5의 Istio 설정은 **같은 조건을 다시 만들기 위한 예시**이며, 실험 당시 파일 그대로가 아닙니다([STATUS.md](./STATUS.md#4-남은-과제)).

## 1. 노드 구성

| 노드 | 사양 | 올라가는 것 |
|---|---|---|
| Master | 6 Core / 12GB / Ubuntu 22.04 | Kubernetes v1.28.15 컨트롤 플레인, Istio 컨트롤 플레인, Jaeger, Prometheus |
| Worker | 6 Core / 12GB / Ubuntu 22.04 | `msa-benchmark` 네임스페이스의 서비스 4개 (+ 사이드카) |
| Load Generator | 6 Core / 12GB / Ubuntu 22.04 | JMeter (클러스터 밖) |

관측 도구를 워커가 아닌 노드에 두려면 Jaeger·Prometheus에도 nodeSelector나 affinity를 걸어 워커를 피하게 합니다.

## 2. 이미지 준비

매니페스트는 레지스트리 없이 노드에 있는 이미지를 쓰도록 되어 있습니다(`image: msa-<서비스>:latest`, `imagePullPolicy: IfNotPresent`).

```bash
# 저장소 루트에서 서비스별 빌드
for s in gateway order inventory user; do
  docker build --build-arg SVC=$s-service -t msa-$s-service:latest .
done
```

빌드한 이미지는 워커 노드의 컨테이너 런타임으로 옮깁니다. 컨테이너 런타임이 containerd라면 예를 들어 다음과 같습니다.

```bash
docker save msa-gateway-service:latest | ssh <워커> sudo ctr -n k8s.io images import -
```

## 3. 공통 배포

1. `k8s/*-service.yaml`의 `nodeAffinity` 값 `node3`을 실제 워커 노드 이름으로 바꿉니다(`kubectl get nodes`).
2. `k8s/configmap-env.yaml`의 `OTLP_TRACES_ENDPOINT`를 클러스터의 Jaeger Collector 주소로 바꿉니다.
3. 배포합니다. 하위 서비스부터 올립니다.

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap-env.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/inventory-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/gateway-service.yaml
kubectl -n msa-benchmark get pods -o wide   # 모두 워커 노드에서 Running·Ready인지
curl http://<워커 IP>:30080/api/chain       # 체인 응답 확인
```

## 4. 조건별 설정

| 조건 | `APP_MODE` | 사이드카 주입 | mTLS 정책 |
|---|---|---|---|
| Step 0 Baseline | `no-auth` | 끔 | — |
| Step 1 JWT Relay | `jwt-relay` | 끔 | — |
| Step 2 Sidecar Only | `no-auth` | 켬 | 끔 (DISABLE) |
| Step 3 Istio mTLS | `no-auth` | 켬 | STRICT |

조건을 바꾼 뒤에는 Pod를 다시 띄웁니다. ConfigMap 변경은 실행 중인 Pod에 반영되지 않습니다.

```bash
kubectl -n msa-benchmark rollout restart deploy
```

### Step 1 — JWT Relay

```bash
kubectl -n msa-benchmark patch configmap msa-benchmark-env -p '{"data":{"APP_MODE":"jwt-relay"}}'
kubectl -n msa-benchmark rollout restart deploy
```

현재 코드에서는 `jwt-relay`일 때 `/actuator/health`도 401을 반환해, 매니페스트의 liveness·readiness 프로브가 실패합니다. 실행 전에 [VERIFICATION.md §3](./VERIFICATION.md#3-알려진-문제)의 해결 방법 중 하나를 적용합니다.

### Step 2 — Sidecar Only

```bash
kubectl label namespace msa-benchmark istio-injection=enabled --overwrite
kubectl -n msa-benchmark rollout restart deploy
kubectl -n msa-benchmark get pods   # READY 2/2 (앱 + istio-proxy)
```

mTLS를 확실히 끕니다. PERMISSIVE 모드는 양쪽에 사이드카가 있으면 자동으로 mTLS를 쓸 수 있습니다.

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: msa-benchmark
spec:
  mtls:
    mode: DISABLE
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: no-mtls
  namespace: msa-benchmark
spec:
  host: "*.msa-benchmark.svc.cluster.local"
  trafficPolicy:
    tls:
      mode: DISABLE
```

### Step 3 — Istio mTLS STRICT

Step 2의 `DestinationRule`을 지우고 `PeerAuthentication`을 STRICT로 바꿉니다.

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: msa-benchmark
spec:
  mtls:
    mode: STRICT
```

외부(JMeter)에서 NodePort로 들어오는 요청은 사이드카가 없는 클라이언트라 STRICT에서 막힐 수 있습니다. 이때는 gateway에만 포트 단위 예외를 두거나(`portLevelMtls`), Istio Ingress Gateway로 받습니다. 어떤 방법을 썼는지 결과와 함께 기록합니다.

적용 확인:

```bash
istioctl x describe pod <order Pod> -n msa-benchmark   # mTLS 모드 확인
```

## 5. 관측 도구

| 도구 | 설정 |
|---|---|
| Jaeger | Collector가 OTLP HTTP(4318)를 받도록 설치. ConfigMap 기본 주소는 `jaeger-collector.observability.svc.cluster.local:4318` |
| Prometheus | 컨테이너 CPU·메모리(cAdvisor/kubelet 메트릭)를 수집. 앱 메트릭은 `/actuator/prometheus` (JWT 모드 주의) |
| 샘플링 | 고부하 측정에서 Jaeger 메모리가 부족하면 `TRACING_SAMPLE_PROBABILITY`를 낮추거나 Jaeger 메모리 저장 한도를 둡니다([RUNBOOK.md RB-01](./RUNBOOK.md#rb-01-jaeger-메모리-증가로-노드가-멈춤)) |

## 6. 정리

```bash
kubectl delete namespace msa-benchmark
```
