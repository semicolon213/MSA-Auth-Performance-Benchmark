# 코드·저장소 규칙

> 개발 단계: **④ 구조 생성** · 산출물: 코딩 규칙, 브랜치·커밋 규칙

## 1. 모듈과 패키지

| 규칙 | 예 |
|---|---|
| 서비스 모듈 이름은 `<도메인>-service` | `order-service` |
| 패키지는 `com.msa.benchmark.<도메인>` 아래 `web`(컨트롤러), `client`(Feign) | `com.msa.benchmark.order.web.OrderChainController` |
| 서비스 공통 코드는 `benchmark-common`, Feign 공통 코드는 `benchmark-feign-common` | |
| 실험 도구는 `benchmark-tools` (서비스 이미지에 넣지 않음) | `JwtTokenGenerator` |
| 체인 컨트롤러 이름은 `<도메인>ChainController`, Feign 클라이언트는 `<다음 도메인>ServiceClient` | `InventoryServiceClient` |

## 2. 설정

- 실험 조건은 코드가 아니라 **환경변수**로 바꿉니다. 새 조건이 필요하면 `application-<모드>.yml` 프로파일을 추가하고 `@ConditionalOnProperty(name = "app.mode")`로 빈을 켭니다.
- 환경변수 이름은 `UPPER_SNAKE_CASE`, YAML 속성은 `kebab-case`입니다(`ARTIFICIAL_LATENCY_MS` ↔ `app.artificial-latency-ms`).
- 4개 서비스의 `application.yml`은 서비스 이름·포트·다음 서비스 주소만 다르게 유지합니다.
- `JWT_SECRET`은 모든 서비스에서 같아야 하며 32바이트 이상입니다.

## 3. 저장소에 올리지 않는 것

`.gitignore`에 있습니다.

| 대상 | 이유 |
|---|---|
| `.env`, `.env.*` (`.env.example` 제외) | 비밀값 |
| `target/` | 빌드 산출물 |
| `results/`, `*.jtl`, `*-report/`, `jmeter.log` | 부하 테스트 결과. 원자료는 별도 측정 시트로 관리 |
| IDE 설정, OS 파일 | 개인 환경 |

## 4. 브랜치와 커밋

- `main` 하나로 작업합니다(2인 프로젝트).
- 커밋 메시지는 한국어로, 무엇을 바꿨는지 한 줄로 씁니다. 예: `메이븐 빌드 오류 수정`
- 실험 조건이나 매니페스트를 바꾸면 결과 문서([RESULTS.md](./RESULTS.md))에 어느 조건에 적용했는지 함께 적습니다.

## 5. 문서

- 단계별 문서 목록은 [docs/README.md](./README.md)에 있습니다.
- 진행 상태와 남은 과제는 [STATUS.md](./STATUS.md)에서만 관리합니다.
- 다이어그램은 Mermaid로 문서 안에 쓰고, 논문 그림은 `docs/images/`에 둡니다.
