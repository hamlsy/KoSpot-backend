# Spring Security Resolver 개선 실행 계획서 (Draft v1)

- 기준 문서: `docs/problem-solve/spring_security_resolver_design_report.md`
- 대상 환경: OSIV OFF / Spring Boot 3.x / JPA
- 목표: 인증/도메인 책임 분리를 통해 detached entity 문제, DB 이중 조회, 예외 계층 문제를 근본적으로 해소
- 채택 패턴: **패턴 A (Resolver -> Long memberId 반환)**

---

## 1. 추진 목표 및 성공 기준

### 1.1 추진 목표

1. `@CurrentMember` 주입값을 `Member` 엔티티에서 `Long memberId`로 전환한다.
2. Resolver의 DB 조회를 완전히 제거한다.
3. JWT 추출/검증 책임을 `JwtAuthenticationFilter`로 단일화한다(헤더/쿠키 포함).
4. 인증 실패를 `401 Unauthorized`로 일관되게 반환한다.

### 1.2 성공 기준 (Definition of Done)

- 인증 필요 API에서 Resolver 레이어 SQL 조회가 0회다.
- UseCase 내부 변경감지(Dirty Checking)가 정상 동작한다.
- 인증 컨텍스트 기반 유스케이스에서 `LazyInitializationException`이 재현되지 않는다.
- 인증 실패 케이스가 `401`로 수렴한다.
- 회귀 테스트 및 통합 테스트를 통과한다.

---

## 2. 핵심 설계 원칙

1. 인증 레이어는 신원 식별(Identity)만 담당한다.
2. 도메인 엔티티 로딩은 반드시 `@Transactional` 경계 내부에서 수행한다.
3. Resolver는 SecurityContext에서 식별자만 추출한다(비즈니스/DB 로직 금지).
4. Filter는 토큰 소스 처리(Header/Cookie)의 단일 책임자가 된다.
5. 예외는 의미 기반 커스텀 예외로 계층화한다.

---

## 3. 목표 아키텍처 (To-Be)

```text
[Filter Layer]     토큰 추출(Header/Cookie), JWT 검증, SecurityContext 등록
[Resolver Layer]   SecurityContext에서 memberId 추출 (DB 조회 없음)
[Controller Layer] memberId(Long) 또는 nullable memberId 전달
[UseCase Layer]    @Transactional 내 엔티티 조회 -> 비즈니스 로직 수행
```

---

## 4. 단계별 실행 계획

### Phase 1 (즉시, 리스크 완화)

- `JwtAuthenticationFilter`에 쿠키 토큰 추출 로직 통합
- `UnauthorizedException` 추가
- `GlobalExceptionHandler`에 401 핸들러 연결
- Resolver 내부 쿠키/JWT 재파싱 분기 제거 준비

**산출물**
- 인증 실패 코드 정합화(500 -> 401)
- 쿠키 파싱 취약점 제거

### Phase 2 (1~2일, 구조 전환)

- `CustomAuthenticationPrincipalArgumentResolver`에서 `memberAdaptor` 의존성 제거
- Resolver 반환 타입을 `Long`으로 변경
- `supportsParameter()`에 타입 강제 검증 추가(`Long` 외 사용 시 기동 단계 예외)

**산출물**
- Resolver 경량화 및 책임 명확화

### Phase 3 (2~5일, 전 구간 마이그레이션)

- 모든 Controller의 `@CurrentMember` 파라미터를 `Long`으로 전환
- 관련 UseCase에 `queryById(memberId)` 명시
- 통합 테스트/회귀 테스트 전체 수행

**산출물**
- detached entity 경로 완전 제거

---

## 5. 작업 패키지 (WBS)

### 5.1 인증 계층

- 토큰 추출 우선순위 규약화(Authorization Bearer 우선, accessToken Cookie 차선)
- 쿠키 파싱을 Servlet Cookie API 기반으로 통일
- SecurityContext principal/name 저장 규약 확정(memberId 파싱 가능 형태)

### 5.2 웹 계층

- `@CurrentMember`, `@CurrentMemberOrNull` 사용처 인벤토리 작성
- Resolver 예외 타입/메시지 표준화

### 5.3 유스케이스 계층

- member 필요 유스케이스의 조회 책임 명시
- 엔티티 변경 로직이 영속 상태에서만 수행되도록 정렬

### 5.4 공통 예외/응답

- Unauthorized 전용 에러 코드 스키마 합의(예: `UNAUTHORIZED`)
- 기존 `RuntimeException("로그인이 필요합니다.")` 사용처 제거

### 5.5 관측성

- 인증 실패/토큰 파싱 실패 로그 레벨 및 메시지 규약 정리
- 모니터링 지표 추가 검토(401 비율, member 조회 쿼리 수)

---

## 6. 테스트 전략

### 6.1 단위 테스트

- Resolver
  - 인증 없음/익명/파싱 실패
  - nullable 허용/비허용 분기
  - 파라미터 타입 검증(`Long`만 허용)
- Filter
  - Bearer 토큰 정상/비정상
  - 쿠키 토큰 정상/비정상
  - 헤더와 쿠키 동시 존재 시 우선순위

### 6.2 통합 테스트

- 인증 필요 API 401/200 분기 검증
- 쓰기 API(예: 닉네임 변경)에서 UPDATE SQL 발생 검증
- 지연 로딩 연관 접근 시 예외 미발생 검증

### 6.3 성능/회귀 점검

- 대표 인증 API SQL 카운트 비교(요청당 member 조회 1회 보장)
- 기존 API 응답 계약(상태코드/바디) 영향 확인

---

## 7. 리스크 및 대응

1. Controller 시그니처 변경으로 컴파일 에러 다발 가능
   - 대응: `@CurrentMember` 사용처 인벤토리 기반 일괄 전환
2. 일부 UseCase의 member 조회 누락 가능
   - 대응: 코드리뷰 체크리스트에 "memberId 입력 시 조회 위치" 항목 의무화
3. principal 포맷 불일치로 Long 파싱 실패 가능
   - 대응: Filter 저장 규약 단일화 + Resolver 경고 로그 + 테스트 고정
4. 500 -> 401 변경에 따른 클라이언트 영향
   - 대응: 사전 공지 및 클라이언트 예외처리 가이드 배포

---

## 8. 배포 및 모니터링 전략

1. 1차 배포: Phase 1 선배포(보안/예외 리스크 즉시 감소)
2. 2차 배포: Phase 2~3 묶음 배포 또는 점진 전개
3. 배포 후 모니터링
   - 401 비율 급증 여부
   - 인증 실패 로그 패턴
   - 주요 API 성공률/지연/SQL 카운트

---

## 9. 코드리뷰 체크리스트

- Resolver에 DB 접근 코드가 없는가
- Filter 외 계층에서 JWT/쿠키 직접 파싱이 없는가
- `@CurrentMember` 파라미터 타입이 `Long`으로 통일되었는가
- UseCase에서 트랜잭션 내부 엔티티 조회가 보장되는가
- Unauthorized 예외가 401로 매핑되는가
- 인증 실패 로그와 시스템 장애 로그가 구분되는가

---

## 10. 권장 일정

- Day 1: Phase 1 완료 + 관련 테스트
- Day 2: Phase 2 완료 + 컴파일/단위 테스트 안정화
- Day 3~5: Phase 3 전수 전환 + 통합/회귀 테스트 + 배포

---

## 부록 A. 이전/이후 비교표

| 항목 | Before | After |
|------|--------|-------|
| Resolver 반환 타입 | `Member` (detached entity) | `Long` (memberId) |
| Resolver 내 DB 조회 | 있음 (1회) | 없음 |
| 쿠키 파싱 위치 | Resolver | JwtAuthenticationFilter |
| 엔티티 조회 위치 | Resolver (트랜잭션 외) | UseCase (트랜잭션 내) |
| 총 DB 조회 횟수/요청 | 2회 | 1회 |
| 변경감지 동작 여부 | 불가 | 정상 |
| Lazy Loading | 불가 | 정상 |
| 인증 실패 HTTP 코드 | 500 가능 | 401 |

## 부록 B. 운영 전환 체크포인트

1. `@CurrentMember`가 붙은 모든 파라미터 타입이 `Long`인지 확인
2. Resolver에서 인프라/도메인 의존성이 제거되었는지 확인
3. 인증 실패 시 예외 응답 포맷이 팀 표준과 일치하는지 확인
4. 대표 트랜잭션 유스케이스의 UPDATE SQL 발생 여부를 로그/테스트로 확인
