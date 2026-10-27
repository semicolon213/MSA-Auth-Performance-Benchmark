# 용어

> 개발 단계: **② 요구사항** · 문서 전체에서 같은 뜻으로 씁니다.

| 용어 | 뜻 |
|---|---|
| MSA | 마이크로서비스 아키텍처. 애플리케이션을 독립 배포되는 작은 서비스로 나눈 구조 |
| 제로 트러스트 (ZTA) | 네트워크 위치와 상관없이 모든 요청을 검증하는 보안 모델 |
| North-South / East-West | 외부↔내부 트래픽 / 내부 서비스 사이 트래픽 |
| JWT | JSON Web Token. 서명된 클레임 묶음. 이 프로젝트는 HS256(공유 비밀키 HMAC) 사용 |
| Token Relay | 받은 토큰을 다음 서비스 호출 헤더에 그대로 실어 보내는 방식 |
| mTLS | 양쪽이 서로 인증서를 확인하는 TLS |
| 서비스 메시 | 서비스 간 통신(라우팅, 보안, 관측)을 인프라에서 처리하는 계층. 여기서는 Istio |
| Envoy 사이드카 | 각 Pod에 함께 뜨는 프록시 컨테이너(`istio-proxy`). 트래픽을 가로채 mTLS를 처리 |
| STRICT / PERMISSIVE / DISABLE | Istio `PeerAuthentication` 모드. 각각 mTLS만 허용 / 평문·mTLS 모두 허용 / mTLS 사용 안 함 |
| 3-Depth | 진입점 Gateway 뒤로 내부 호출이 3단계(Order, Inventory, User) 이어지는 구조 |
| Step 0~3 | 실험 조건. Baseline, JWT Relay, Sidecar Only, Istio mTLS ([EXPERIMENT_DESIGN.md](./EXPERIMENT_DESIGN.md#2-실험-조건)) |
| `APP_MODE` | 애플리케이션 인증 모드. `no-auth` 또는 `jwt-relay`. Spring 프로파일과 같은 값 |
| TC 1~3 | 부하 케이스. 동시 스레드 30 / 300 / 800 |
| TPS | 초당 처리한 요청 수 |
| E2E 지연 시간 | JMeter가 요청을 보내고 Gateway 응답을 받을 때까지 걸린 시간 |
| p99 | 응답 시간을 정렬했을 때 99번째 백분위 값 |
| m (millicore) | CPU 1코어의 1/1000. 1,000m = 1코어 |
| 스팬(span) | 분산 추적에서 한 구간(서비스 처리, 호출 등)의 시작·종료 기록 |
| OTLP | OpenTelemetry 전송 프로토콜. 여기서는 HTTP 4318 포트 |
| Startup Wait / Warm-up / Main Test | 측정 3단계. 안정화 대기 / 사전 부하 / 60초 본 측정 |
| 대표 회차 | 3회 측정의 평균에 가장 가까운 한 회. 결과 비교에 사용 |
