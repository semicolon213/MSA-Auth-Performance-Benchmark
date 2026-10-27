# 문서 안내

개발 단계별 산출물을 정리한 문서 모음입니다. 처음이라면 **역할별 시작점**부터 보세요.

## 역할별 시작점

| 나는 | 먼저 읽을 문서 |
|---|---|
| 프로젝트를 처음 보는 사람 | [루트 README](../README.md) → [RESULTS](./RESULTS.md) → [EXPERIMENT_DESIGN](./EXPERIMENT_DESIGN.md) |
| 연구 내용을 검토하는 사람 | [PROBLEM_DEFINITION](./PROBLEM_DEFINITION.md) → [EXPERIMENT_DESIGN](./EXPERIMENT_DESIGN.md) → [RESULTS](./RESULTS.md) §5 타당성 검토 |
| 실험을 다시 돌려 볼 사람 | [실험 사용법 가이드](./실험-사용법-가이드.md) (로컬) → [DEPLOYMENT](./DEPLOYMENT.md) (Kubernetes·Istio) → [RUNBOOK](./RUNBOOK.md) |
| 코드를 고칠 사람 | [ARCHITECTURE](./ARCHITECTURE.md) → [API_SPEC](./API_SPEC.md) → [CONVENTIONS](./CONVENTIONS.md) → [VERIFICATION](./VERIFICATION.md) |

## 개발 단계별 산출물

```mermaid
flowchart LR
    S1["① 문제 정의"] --> S2["② 요구사항"] --> S3["③ 설계"] --> S4["④ 구조 생성"] --> S5["⑤ 구현"] --> S6["⑥ 테스트·측정"] --> S7["⑦ 수정·문서화"]
```

| 단계 | 산출물 | UML 다이어그램 |
|---|---|---|
| ① 문제 정의 | [PROBLEM_DEFINITION](./PROBLEM_DEFINITION.md): 배경, 연구 질문, 목표, 범위, 제약 | [Activity](./PROBLEM_DEFINITION.md#2-문제) (문제 도출 흐름) |
| ② 요구사항 | [REQUIREMENTS](./REQUIREMENTS.md): 액터, 기능·비기능 요구사항(MoSCoW) · [GLOSSARY](./GLOSSARY.md) | [Use Case](./REQUIREMENTS.md#2-use-case-다이어그램) |
| ③ 설계 | [EXPERIMENT_DESIGN](./EXPERIMENT_DESIGN.md): 조건, 부하, 지표, 절차 · [API_SPEC](./API_SPEC.md) | [Sequence](./ARCHITECTURE.md#4-sequence-다이어그램) · [Class](./ARCHITECTURE.md#5-class-다이어그램) |
| ④ 구조 생성 | [ARCHITECTURE](./ARCHITECTURE.md) · [ADR](./ADR.md) · [CONVENTIONS](./CONVENTIONS.md) · [DEPLOYMENT](./DEPLOYMENT.md) | [Component](./ARCHITECTURE.md#2-component-다이어그램) · [Deployment](./ARCHITECTURE.md#3-deployment-다이어그램) |
| ⑤ 구현 | 소스 코드 · [실험 사용법 가이드](./실험-사용법-가이드.md) · [STATUS](./STATUS.md) | [State](./EXPERIMENT_DESIGN.md#5-측정-절차) (측정 절차) |
| ⑥ 테스트·측정 | [RESULTS](./RESULTS.md): 측정 결과·분석 · [VERIFICATION](./VERIFICATION.md): 코드 검증 | [Communication](./ARCHITECTURE.md#7-communication-다이어그램-step-3-요청-1건) |
| ⑦ 수정·문서화 | [RUNBOOK](./RUNBOOK.md): 장애 대응 · [STATUS](./STATUS.md): 남은 과제 | [Package](./ARCHITECTURE.md#8-package-다이어그램-maven-모듈) |

데이터베이스가 없는 프로젝트라 ERD는 만들지 않았습니다.

## 문서 목록

| 문서 | 내용 |
|---|---|
| [PROBLEM_DEFINITION.md](./PROBLEM_DEFINITION.md) | 왜 이 연구를 했나, 무엇을 묻나 |
| [REQUIREMENTS.md](./REQUIREMENTS.md) | 실험 환경이 갖춰야 할 기능과 타당성 조건 |
| [GLOSSARY.md](./GLOSSARY.md) | 용어 |
| [EXPERIMENT_DESIGN.md](./EXPERIMENT_DESIGN.md) | Step 0~3, 부하 케이스, 지표, 측정 절차, 계획서 대비 변경 |
| [API_SPEC.md](./API_SPEC.md) | 엔드포인트, 응답 형식, 서비스 간 호출 |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 구성 요소, 배포 구조, 호출 흐름, 클래스, 설정, 모듈 |
| [ADR.md](./ADR.md) | 설계 결정 10건과 그 이유 |
| [CONVENTIONS.md](./CONVENTIONS.md) | 모듈·패키지·설정·커밋 규칙 |
| [DEPLOYMENT.md](./DEPLOYMENT.md) | Kubernetes·Istio 조건별 배포 |
| [실험-사용법-가이드.md](./실험-사용법-가이드.md) | Ubuntu에서 설치부터 로컬 실험까지 단계별 안내 |
| [RESULTS.md](./RESULTS.md) | TPS, 지연, 순수 mTLS 비용, 컨테이너별 CPU, 타당성 검토 |
| [VERIFICATION.md](./VERIFICATION.md) | 빌드·동작 확인, 알려진 문제 |
| [RUNBOOK.md](./RUNBOOK.md) | 실험 중 문제 유형별 대응 |
| [STATUS.md](./STATUS.md) | 구성 요소별 상태, 일정, 남은 과제 |
