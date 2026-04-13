# Grafana Alloy Minimal Plan for KoSpot Backend

## 목표
- KoSpot Backend가 제공하는 Prometheus endpoint를 가장 가볍게 Grafana Cloud로 전송한다.
- 이 서버에는 Grafana Alloy만 추가하고, 별도 Prometheus 서버는 두지 않는다.
- 운영 복잡도와 리소스 사용량을 최소화한다.

## 대상 구조
- Spring Boot 앱이 `/actuator/prometheus` 를 제공한다.
- Grafana Alloy가 같은 서버에서 해당 endpoint를 scrape 한다.
- Grafana Alloy가 수집한 메트릭을 Grafana Cloud로 `remote_write` 한다.

## 왜 이 방식이 최적인가
- 별도 Prometheus 저장소/서버 운영이 필요 없다.
- 프리티어 또는 소규모 서버에 가장 가볍다.
- 앱 endpoint를 외부에 직접 공개하지 않고 `localhost` 기준으로 수집할 수 있다.
- Grafana Cloud의 대시보드/알림 기능을 바로 활용할 수 있다.

## 전제 조건
- 애플리케이션이 운영 환경에서 `/actuator/prometheus` 를 정상 제공해야 한다.
- 운영 환경은 context-path prefix가 없으므로 scrape 경로는 `/actuator/prometheus` 다.
- Grafana Cloud에서 Metrics endpoint와 Access Policy Token을 발급받아야 한다.

## 작업 범위

### 1. Grafana Cloud 준비
- `metrics:write` 권한이 있는 Access Policy Token 생성
- Prometheus remote_write endpoint 확인
- 스택 ID 또는 사용자명 형식 계정 정보 확인

## 2. 서버 준비
- 서버에 Grafana Alloy 설치
- systemd 서비스로 등록해 부팅 시 자동 시작되도록 구성
- Alloy 설정 파일 위치를 표준 경로로 관리

권장 예시:
- 설정 파일: `/etc/alloy/config.alloy`
- 서비스 이름: `alloy`

## 3. 최소 Alloy 설정
- scrape 대상은 KoSpot Backend의 로컬 actuator endpoint 하나만 둔다
- remote_write 대상은 Grafana Cloud metrics endpoint 하나만 둔다
- 인증은 Grafana Cloud Access Policy Token을 사용한다

최소 구성 개념:
- `prometheus.scrape`
- `prometheus.remote_write`

scrape 대상:
- 운영: `http://localhost:8080/actuator/prometheus`

로컬 테스트 시 참고:
- local 프로필은 context-path가 `/api` 이므로
- `http://localhost:8080/api/actuator/prometheus`

## 4. 운영 보안 원칙
- actuator endpoint는 가능하면 외부 전체 공개 대신 내부 접근 위주로 유지
- Alloy와 앱이 같은 서버에 있으므로 `localhost` 수집을 우선 사용
- Grafana Cloud 토큰은 설정 파일에 평문 하드코딩하지 말고 환경변수 또는 시크릿 방식으로 주입

## 5. 검증 절차
- Spring 앱에서 `/actuator/prometheus` 응답 확인
- Alloy 서비스 기동 확인
- Alloy 로그에서 scrape/remote_write 에러 없는지 확인
- Grafana Cloud Metrics Explorer에서 `application="kospot-backend"` 태그로 메트릭 유입 확인
- 대표 메트릭 예시 확인
- `up`
- `jvm_memory_used_bytes`
- `process_cpu_usage`
- `http_server_requests_seconds_count`

## 6. 장애 포인트 체크
- 앱이 `localhost:8080` 이 아닌 다른 포트에서 떠 있으면 scrape target 수정 필요
- 운영 방화벽과 무관하게 `localhost` scrape 는 가능하지만, outbound 로 Grafana Cloud endpoint 에 HTTPS 연결은 가능해야 한다
- 토큰 scope 가 맞지 않으면 remote_write 가 실패한다
- endpoint 경로를 `/actuator/prometheus` 와 `/api/actuator/prometheus` 중 잘못 잡으면 scrape 실패한다

## 7. 완료 기준
- 서버에 Grafana Alloy가 설치되어 있다
- Alloy가 부팅 후 자동 시작된다
- Alloy가 `localhost` 의 Prometheus endpoint 를 scrape 한다
- Grafana Cloud에 메트릭이 실제로 유입된다
- 운영 문서에 token 관리, 설정 위치, 재시작 방법이 정리된다

## 8. 후속 권장 작업
- Alloy 설정 파일을 배포 스크립트 또는 IaC에 포함
- Grafana Cloud 대시보드 기본 템플릿 구성
- CPU/메모리/응답시간/에러율 알림 규칙 추가
- 필요 시 node_exporter 계열 시스템 메트릭도 Alloy로 함께 수집 검토

## 9. 구현 메모
- 지금 단계의 최우선 목표는 "앱 메트릭을 Grafana Cloud로 보내기" 이다
- 처음부터 로그/트레이스까지 함께 붙이지 않는다
- 처음 구성은 단일 scrape target 으로 단순하게 시작하고, 안정화 후 확장한다
