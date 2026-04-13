# KoSpot Backend Prometheus Endpoint Plan (Grafana Cloud 연동 전제)

## 1) 목표
- 현재 인스턴스에 Prometheus 컨테이너를 추가하지 않고, Spring Boot 앱이 메트릭 endpoint를 안정적으로 제공한다.
- 외부 수집기(예: Grafana Cloud 연동용 Agent/Alloy/Prometheus)가 수집할 수 있는 최소 요건을 만든다.
- 운영 노출 범위를 최소화하고, 로컬/운영 경로 차이로 인한 장애를 방지한다.

## 2) 현재 상태 요약
- `build.gradle`에 `spring-boot-starter-actuator`, `micrometer-registry-prometheus` 의존성이 없다.
- `Dockerfile`, `docker-compose.yml`은 `/actuator/health` 헬스체크를 이미 사용 중이다.
- `application-local.yml`에는 `management.*` 설정이 일부 있고, `server.servlet.context-path: /api`가 있다.
- `application.yml`에는 운영 공통 `management.*`가 아직 없다.
- `SecurityConfig`는 actuator 정책이 명시적이지 않다.

## 3) 구현 원칙
- 애플리케이션은 메트릭을 노출만 한다. 수집/저장은 외부 시스템이 담당한다.
- actuator는 필요한 endpoint만 노출한다 (`health`, `prometheus`).
- 공통 정책은 `application.yml`, 로컬 편의 설정은 `application-local.yml`로 분리한다.
- 운영에서 `/actuator/**` 전체 공개는 지양한다.

## 4) 실행 계획

### Step 1. 의존성 추가
`build.gradle`:
- `org.springframework.boot:spring-boot-starter-actuator`
- `io.micrometer:micrometer-registry-prometheus`

### Step 2. 공통 management 설정 추가
`src/main/resources/application.yml`에 아래 정책 반영:
- `management.endpoints.web.exposure.include=health,prometheus`
- `management.prometheus.metrics.export.enabled=true`
- `management.metrics.tags.application=kospot-backend`
- `management.endpoint.health.show-details`는 아래 중 하나로 결정
  - 운영 우선 안전: `never`
  - 인증 체계 정리 후: `when-authorized`

### Step 3. 로컬 설정 정리
`src/main/resources/application-local.yml`:
- 공통으로 올라간 항목은 중복 제거
- 로컬 디버깅이 필요하면 `metrics`, `info`만 추가 노출

### Step 4. Security 정책 명시
`SecurityConfig`에 actuator 경로 의도를 코드로 남긴다.
- 최소 허용 대상: `/actuator/health`, `/actuator/prometheus`
- 향후 보안 강화 시에도 actuator 정책이 누락되지 않도록 matcher를 분리

### Step 5. 경로 검증 (중요)
`local` 프로필은 `context-path: /api`이므로 실제 확인 경로가 다르다.
- 로컬: `/api/actuator/health`, `/api/actuator/prometheus`
- 운영(기본 context-path): `/actuator/health`, `/actuator/prometheus`

### Step 6. Docker 헬스체크 정합성 확인
- 실제 앱 context-path와 헬스체크 URL이 일치하는지 확인
- 필요하면 `Dockerfile`, `docker-compose.yml`의 healthcheck URL을 프로필 기준으로 분리/조정

### Step 7. 검증 체크리스트
- 앱 기동 후 health endpoint 200 확인
- Prometheus endpoint 텍스트 응답 확인
- 주요 메트릭 확인: `jvm_`, `process_`, `system_`, `http_server_requests`
- 애플리케이션 태그(`application=kospot-backend`) 노출 확인

## 5) Grafana Cloud 연동 전제에서의 주의사항
- Grafana Cloud는 private endpoint를 직접 scrape하지 못하는 경우가 많다.
- 일반적으로 아래 구조를 사용한다.
  - 앱이 endpoint 제공
  - 같은 네트워크의 수집기(Agent/Alloy/Prometheus)가 scrape
  - 수집기가 Grafana Cloud로 전송
- 따라서 이번 이슈 범위는 endpoint 제공까지로 제한하고,
  Grafana Cloud ingest/agent 배포는 별도 이슈로 분리하는 것이 안전하다.

## 6) 완료 기준 (Definition of Done)
- `build.gradle`에 actuator/prometheus 의존성이 반영됨
- 프로필별 실제 경로에서 health/prometheus 응답 확인 완료
- 공통/로컬 설정 책임이 분리됨
- actuator 접근 정책이 코드에 명시됨
- README 또는 운영 문서에 endpoint 및 점검 방법이 반영됨

## 7) 후속 이슈 권장
- Grafana Cloud Agent/Alloy 배포 및 scrape 구성
- 네트워크 접근 제어(allowlist/보안그룹) 정책 확정
- 고카디널리티 태그 관리 정책 수립
