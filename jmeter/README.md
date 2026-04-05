# JMeter 샘플

## 파일

| 파일 | 용도 |
|------|------|
| `msa-chain-no-auth.jmx` | `APP_MODE=no-auth` — `Authorization` 헤더 없음 |
| `msa-chain-jwt.jmx` | `APP_MODE=jwt-relay` — `Authorization: Bearer ${JWT_TOKEN}` |

공통 설정:

- **스레드:** 10명, 램프업 30초, 스케줄러 **300초**(5분) 지속, 무한 루프(스케줄러로 종료)
- **요청:** `GET http://${HOST}:${PORT}/api/chain`

논문 계획서의 웜업·부하 단계는 스레드 수·램프업·지속 시간을 GUI에서 조정하면 됩니다.

## 비 GUI 실행

```bash
jmeter -n -t jmeter/msa-chain-no-auth.jmx -l results/no-auth.jtl -e -o results/no-auth-report
```

JWT 모드에서 토큰을 CLI로 넘기기:

```bash
jmeter -n -t jmeter/msa-chain-jwt.jmx -JJWT_TOKEN=eyJhbGciOiJIUzI1NiJ9... -l results/jwt.jtl
```

`JWT_TOKEN`은 `benchmark-tools`의 `JwtTokenGenerator`로 생성합니다.

## 변수

| 변수 | 기본 | 설명 |
|------|------|------|
| `HOST` | localhost | Gateway 호스트 |
| `PORT` | 8080 | Gateway 포트 |
| `JWT_TOKEN` | (빈 값) | jwt JMX 전용, GUI에서 붙여넣거나 `-J`로 설정 |
