# Spring Boot Security Resolver 설계 분석 보고서 및 개선 제안서

> **작성 기준**: OSIV(Open Session In View) OFF 환경 / Spring Boot 3.x / JPA  
> **대상 코드**: `CustomAuthenticationPrincipalArgumentResolver` + `@CurrentMember` 어노테이션 패턴  
> **심각도 등급**: 🔴 Critical / 🟠 Major / 🟡 Minor

---

## 목차

1. [현재 설계 구조 분석](#1-현재-설계-구조-분석)
2. [문제점 상세 진단](#2-문제점-상세-진단)
3. [업계 표준 패턴 비교](#3-업계-표준-패턴-비교)
4. [개선 제안](#4-개선-제안)
5. [마이그레이션 전략](#5-마이그레이션-전략)
6. [최종 결론](#6-최종-결론)

---

## 1. 현재 설계 구조 분석

### 1.1 요청 흐름 (As-Is)

```
HTTP Request
    │
    ▼
[JwtAuthenticationFilter]          ← SecurityContext에 Authentication 등록
    │
    ▼
[DispatcherServlet]
    │
    ▼
[CustomAuthenticationPrincipalArgumentResolver]
    │  ① Authentication에서 memberId 추출
    │  ② memberAdaptor.queryById(memberId)  ← ⚠️ 트랜잭션 없음, 영속성 컨텍스트 없음
    │  ③ detached Member 반환
    ▼
[Controller]
    │  detached Member 객체를 UseCase로 전달
    ▼
[UseCase / Service]  @Transactional
    │  새로운 영속성 컨텍스트 생성
    │  ← 전달받은 Member는 이 컨텍스트 소속이 아님 (detached)
    │  ← 변경감지(Dirty Checking) 불가 ❌
    │  ← Lazy Loading 불가 ❌
    ▼
[Repository / DB]
```

### 1.2 현재 코드의 책임 분포

| 클래스 | 현재 담당 책임 | 원래 가져야 할 책임 |
|--------|---------------|-------------------|
| `CustomAuthenticationPrincipalArgumentResolver` | 토큰 파싱, 쿠키 파싱, DB 조회, 엔티티 반환 | SecurityContext에서 인증 정보 추출 |
| `JwtAuthenticationFilter` | SecurityContext 등록 | 토큰 파싱, 검증, SecurityContext 등록 |
| `UseCase` | 비즈니스 로직 (엔티티 조회 없음) | 트랜잭션 내 엔티티 조회 + 비즈니스 로직 |

---

## 2. 문제점 상세 진단

### 🔴 [Critical] 문제 1: OSIV OFF 환경에서의 Detached Entity 전달

#### 원인 분석

OSIV가 **ON**일 때는 HTTP 요청 전체 생명주기 동안 영속성 컨텍스트가 열려 있어 어디서든 엔티티가 관리 상태를 유지합니다.  
OSIV가 **OFF**가 되는 순간, **영속성 컨텍스트의 생명주기 = 트랜잭션의 생명주기**로 엄격히 제한됩니다.

```
OSIV ON  (기존):  [Request 시작 ─────────── 영속성 컨텍스트 유지 ─────────── Response 종료]
OSIV OFF (현재):  [  ...  ]  [@Transactional 시작 ── 영속성 컨텍스트 ── @Transactional 종료]  [  ...  ]
```

`HandlerMethodArgumentResolver`의 `resolveArgument()`는 **트랜잭션 바깥**에서 실행됩니다.  
따라서 `memberAdaptor.queryById(memberId)`로 조회된 `Member`는 메서드 종료 즉시 영속성 컨텍스트에서 분리(detach)됩니다.

#### 실제 장애 시나리오

```java
// UseCase (트랜잭션 내부)
@Transactional
public void updateNickname(Member member, String newNickname) {
    member.updateNickname(newNickname); // ← detached 상태이므로 변경감지 무반응
    // flush 시점에 UPDATE 쿼리 미발생 → 데이터 변경 안 됨, 예외도 없음
    // 가장 위험한 유형: 조용한 실패(Silent Failure)
}
```

```java
// Lazy Loading 장애
@Transactional
public void process(Member member) {
    List<Post> posts = member.getPosts(); // ← LazyInitializationException 발생
    // "could not initialize proxy - no Session"
}
```

**특히 위험한 것은 변경감지 실패가 예외를 던지지 않는다는 점입니다.** 데이터는 저장되지 않지만 API는 200 OK를 반환합니다. 운영 환경에서 발견하기 극히 어려운 버그입니다.

---

### 🔴 [Critical] 문제 2: Resolver의 책임 과부하 (God Object 안티패턴)

현재 `resolveArgument()` 단일 메서드가 수행하는 책임:

```
① SecurityContext에서 Authentication 추출          → 인증 계층 책임 (적절)
② Authentication에서 memberId 파싱                → 인증 계층 책임 (적절)
③ memberId가 없으면 쿠키 헤더 직접 파싱           → Filter 계층 책임 (부적절)
④ 쿠키에서 JWT 토큰 문자열 추출                   → Filter 계층 책임 (부적절)
⑤ JWT 토큰 파싱 및 memberId 재추출               → Filter 계층 책임 (부적절)
⑥ memberAdaptor.queryById()로 DB 조회             → Domain/UseCase 계층 책임 (부적절)
⑦ Member 엔티티 반환                              → Domain 계층 책임 (부적절)
```

**7개의 책임 중 5개가 잘못된 레이어에 위치**합니다.

#### Spring Security 필터 체인과의 책임 중복 문제

```java
// Resolver 내부에 Filter가 해야 할 일이 침투
if (memberId == null) {
    String token = extractTokenFromCookies(webRequest); // ← Filter 책임
    if (token != null) {
        memberId = tokenService.getMemberIdFromToken(token); // ← Filter 책임
    }
}
```

이는 `JwtAuthenticationFilter`가 쿠키 기반 토큰을 처리하지 못하고 있음을 Resolver가 **방어적으로 보완**하는 코드입니다. Filter의 미완성을 Resolver가 땜질하는 구조로, 두 곳에 같은 책임이 분산되어 유지보수 시 어느 쪽을 수정해야 할지 불명확해집니다.

---

### 🔴 [Critical] 문제 3: DB 이중 조회 (N+1의 변형)

```
[Resolver]  → SELECT * FROM member WHERE id = ?   (1회)
    ↓ detached Member 전달
[UseCase]   → SELECT * FROM member WHERE id = ?   (2회, 재조회 불가피)
```

Resolver에서 조회한 엔티티는 UseCase에서 사용할 수 없으므로, 올바르게 작성된 UseCase라면 반드시 재조회합니다. **모든 인증 필요 API에서 DB 조회가 1회 추가 발생**하는 구조입니다.

트래픽이 초당 1,000 req/s인 서비스라면, 이 버그 하나로 DB에 초당 1,000건의 불필요한 쿼리가 발생합니다.

---

### 🟠 [Major] 문제 4: 쿠키 파싱 로직의 취약성

```java
// 현재 코드
String[] parts = cookie.trim().split("=");
if (parts.length == 2 && parts[0].equals(cookieName)) {
    String token = parts[1];
```

JWT 값 자체에 `=`이 포함될 수 있습니다 (Base64 패딩). `split("=")` 사용 시 토큰이 잘립니다.

```
// Base64 패딩 포함 JWT 예시
accessToken=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w==

// split("=") 결과
parts[0] = "accessToken"
parts[1] = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w"
parts[2] = ""  ← parts.length != 2 조건에 걸려 토큰 파싱 실패
```

표준 `javax.servlet.http.Cookie` API나 `split("=", 2)`를 사용해야 합니다.

---

### 🟠 [Major] 문제 5: `RuntimeException` 사용으로 인한 예외 계층 파괴

```java
// 현재 코드
throw new RuntimeException("로그인이 필요합니다.");
```

`RuntimeException`을 그대로 던지면:
- Spring의 `@ExceptionHandler`에서 의도적으로 핸들링하기 어렵습니다
- HTTP 상태코드 매핑이 불가능합니다 (500으로 응답될 가능성)
- `401 Unauthorized`를 반환해야 할 곳에서 `500 Internal Server Error`가 반환됩니다
- 로그에서 실제 장애와 인증 실패를 구분할 수 없습니다

---

### 🟡 [Minor] 문제 6: `principal` 변수 선언 후 미사용

```java
Object principal = authentication.getPrincipal(); // ← 선언
Long memberId = extractMemberIdFromAuthentication(authentication);
// principal은 이후 사용되지 않음 → Dead Code
```

---

### 🟡 [Minor] 문제 7: 애드센스 크롤러 주석의 의미 오염

```java
// 애드센스 크롤러용 쿠키명들 (JSESSIONID, accessToken, authToken 등)
```

`JSESSIONID`는 서블릿 세션 ID이며 JWT와 전혀 관계가 없습니다. 이를 JWT처럼 파싱 시도하면 파싱에 실패하지만, 코드 의도를 오해하게 만들고 기술 부채를 형성합니다.

---

## 3. 업계 표준 패턴 비교

### 3.1 패턴 비교표

| 패턴 | 개요 | OSIV OFF 호환 | DB 조회 횟수 | 권장 여부 |
|------|------|:---:|:---:|:---:|
| **현재**: Resolver → Entity 반환 | Resolver에서 엔티티 직접 조회/반환 | ❌ | 2회 | ❌ |
| **패턴 A**: Resolver → MemberId 반환 | Resolver는 ID만 반환, UseCase에서 조회 | ✅ | 1회 | ✅ 권장 |
| **패턴 B**: Resolver → UserDetails 반환 | Spring Security 표준 `UserDetails` 활용 | ✅ | 1회 | ✅ 권장 |
| **패턴 C**: UseCase에서 직접 SecurityContext 접근 | Resolver 없이 UseCase가 직접 추출 | ✅ | 1회 | 🔺 조건부 |

### 3.2 패턴 A 상세: Resolver → `MemberId(Long)` 반환 (최권장)

가장 단순하고 명확한 패턴입니다. 인증 레이어는 "누가 요청했는가(ID)"만 판별하고, 엔티티 로딩 책임을 완전히 비즈니스 레이어로 위임합니다.

```
Resolver: Authentication → Long memberId (DB 조회 없음)
    ↓
Controller: memberId 수신
    ↓
UseCase @Transactional: queryById(memberId) → 영속 Member → 변경감지 ✅
```

**장점**:
- 구조가 단순하여 신입 개발자도 직관적으로 이해 가능
- OSIV ON/OFF 무관하게 동일하게 동작
- DB 조회 1회로 최소화

**단점**:
- 컨트롤러 시그니처가 `Long memberId`로 변경되어 엔티티 타입 힌트 소실
- UseCase 모든 메서드에서 `queryById()` 호출이 반복될 수 있음

### 3.3 패턴 B 상세: `CustomUserDetails` 활용

Spring Security의 `UserDetails`를 구현한 `CustomUserDetails`를 `Principal`로 등록하고, Resolver가 이를 반환합니다. 단, 여기서 `CustomUserDetails`는 **엔티티가 아닌 VO(Value Object)** 여야 합니다.

```java
// UserDetails를 구현한 경량 VO (엔티티 아님)
public record MemberPrincipal(Long memberId, String email, Set<Role> roles)
        implements UserDetails {
    // ... UserDetails 메서드 구현
}
```

```
Filter: JWT 파싱 → MemberPrincipal(id, email, roles) → SecurityContext
    ↓
Resolver: SecurityContext → MemberPrincipal 반환 (DB 조회 없음)
    ↓
Controller: MemberPrincipal 수신
    ↓
UseCase @Transactional: queryById(principal.memberId()) → 영속 Member ✅
```

**장점**:
- 컨트롤러에서 `principal.email()` 같은 부가 정보 즉시 접근 가능 (DB 조회 없이)
- Spring Security 표준 스타일

---

## 4. 개선 제안

### 4.1 레이어별 책임 재배분

```
[Filter Layer]     토큰 추출(Header/Cookie), JWT 검증, SecurityContext 등록
[Resolver Layer]   SecurityContext에서 ID 또는 Principal VO 추출 (DB 조회 없음)
[Controller Layer] ID 또는 Principal VO를 UseCase로 전달
[UseCase Layer]    @Transactional 내에서 엔티티 조회 → 비즈니스 로직
```

### 4.2 `JwtAuthenticationFilter` 개선 (쿠키 처리 통합)

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null && tokenService.validateToken(token)) {
                Long memberId = tokenService.getMemberIdFromToken(token);
                setAuthentication(memberId);
            }
        } catch (Exception e) {
            log.warn("JWT 인증 처리 중 오류 (무시): {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 추출 우선순위:
     * 1. Authorization: Bearer {token} 헤더
     * 2. accessToken 쿠키
     */
    private String resolveToken(HttpServletRequest request) {
        // 1순위: Authorization Header
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // 2순위: Cookie (표준 API 사용 - = 파싱 버그 없음)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                .filter(c -> "accessToken".equals(c.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
        }

        return null;
    }

    private void setAuthentication(Long memberId) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                memberId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
```

### 4.3 `CustomAuthenticationPrincipalArgumentResolver` 개선 (경량화)

```java
@Slf4j
@Component
public final class CustomAuthenticationPrincipalArgumentResolver
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasCurrentMember = parameter.hasParameterAnnotation(CurrentMember.class);
        boolean hasCurrentMemberOrNull = parameter.hasParameterAnnotation(CurrentMemberOrNull.class);
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());

        if ((hasCurrentMember || hasCurrentMemberOrNull) && !isLongType) {
            // 컴파일 타임에 잡을 수 없으므로 기동 시점에 명확한 예외 발생
            throw new IllegalStateException(
                "@CurrentMember / @CurrentMemberOrNull 은 Long 타입 파라미터에만 사용할 수 있습니다. "
                + "문제 파라미터: " + parameter.getDeclaringClass().getSimpleName()
                + "#" + parameter.getMethod().getName()
            );
        }

        return isLongType && (hasCurrentMember || hasCurrentMemberOrNull);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        boolean nullable = parameter.hasParameterAnnotation(CurrentMemberOrNull.class);

        Long memberId = extractMemberIdFromSecurityContext();

        if (memberId == null) {
            if (nullable) return null;
            throw new UnauthorizedException("로그인이 필요합니다.");
        }

        return memberId;
    }

    private Long extractMemberIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        String name = authentication.getName();
        if (!StringUtils.hasText(name) || "anonymousUser".equals(name)) {
            return null;
        }

        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            log.warn("Authentication name을 memberId로 변환 실패: name={}", name);
            return null;
        }
    }
}
```

### 4.4 커스텀 예외 클래스 정의

```java
// 401 Unauthorized 전용 예외
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

// GlobalExceptionHandler에서 명시적 처리
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.of("UNAUTHORIZED", e.getMessage()));
}
```

### 4.5 Controller 및 UseCase 사용 예시

```java
// Controller: Long memberId를 받아 UseCase로 전달
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final UpdateNicknameUseCase updateNicknameUseCase;
    private final GetMyProfileUseCase getMyProfileUseCase;

    @PatchMapping("/nickname")
    public ResponseEntity<Void> updateNickname(
            @CurrentMember Long memberId,       // ← Long 타입
            @RequestBody UpdateNicknameRequest request) {
        updateNicknameUseCase.execute(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile(
            @CurrentMemberOrNull Long memberId) { // ← nullable
        return ResponseEntity.ok(getMyProfileUseCase.execute(memberId));
    }
}
```

```java
// UseCase: 트랜잭션 내에서 엔티티 조회 → 변경감지 정상 동작
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateNicknameUseCase {

    private final MemberAdaptor memberAdaptor;

    public void execute(Long memberId, UpdateNicknameRequest request) {
        Member member = memberAdaptor.queryById(memberId); // ← 트랜잭션 내 조회, 영속 상태
        member.updateNickname(request.nickname());         // ← 변경감지 정상 동작 ✅
        // 트랜잭션 종료 시점에 자동 flush → UPDATE 쿼리 발생
    }
}
```

---

## 5. 마이그레이션 전략

### 5.1 단계별 마이그레이션 (브레이킹 체인지 최소화)

```
Phase 1 (즉시)
  ├── JwtAuthenticationFilter에 쿠키 토큰 추출 로직 이전
  ├── UnauthorizedException 커스텀 예외 클래스 생성
  └── GlobalExceptionHandler에 401 핸들러 추가

Phase 2 (1~2일)
  ├── Resolver에서 memberAdaptor 의존성 제거
  ├── Resolver 반환 타입을 Long으로 변경
  └── supportsParameter에 타입 검증 추가

Phase 3 (2~5일)
  ├── 모든 Controller의 @CurrentMember 파라미터 타입을 Long으로 변경
  ├── 각 UseCase에 queryById() 호출 추가
  └── 통합 테스트 전체 수행
```

### 5.2 이전/이후 코드 비교

| 항목 | Before | After |
|------|--------|-------|
| Resolver 반환 타입 | `Member` (detached entity) | `Long` (memberId) |
| Resolver 내 DB 조회 | 있음 (1회) | 없음 |
| 쿠키 파싱 위치 | Resolver | JwtAuthenticationFilter |
| 엔티티 조회 위치 | Resolver (트랜잭션 外) | UseCase (트랜잭션 內) |
| 총 DB 조회 횟수/요청 | 2회 | 1회 |
| 변경감지 동작 여부 | ❌ 불가 | ✅ 정상 |
| Lazy Loading | ❌ 불가 | ✅ 정상 |
| 인증 실패 HTTP 코드 | 500 (RuntimeException) | 401 (UnauthorizedException) |
| Resolver 코드 라인 수 | ~90줄 | ~40줄 |

---

## 6. 최종 결론

### 근본 원칙

> **"인증(Authentication) 레이어는 신원(Identity)을 식별하는 것으로 책임을 끝내야 한다.  
> 도메인 객체 로딩은 반드시 트랜잭션 경계 내부, 즉 비즈니스 레이어의 책임이다."**

OSIV는 이 원칙 위반을 **감춰주는 마취제**였습니다. OSIV를 끄면 잘못된 설계가 즉시 장애로 드러납니다. 이번 문제는 OSIV가 원인이 아니라, OSIV 덕분에 숨겨져 있던 **레이어 책임 위반**이 수면 위로 드러난 것입니다.

### 핵심 요약

```
❌ 잘못된 설계
Resolver → Member 조회(트랜잭션 外) → detached Entity → UseCase → 변경감지 불가

✅ 올바른 설계  
Filter  → JWT 파싱 → SecurityContext 등록
Resolver → memberId 추출 (DB 조회 없음)
UseCase → @Transactional → Member 조회(트랜잭션 內) → 변경감지 ✅
```

각 레이어가 자신의 책임만 가질 때, 코드는 단순해지고 장애는 줄어들며 테스트는 쉬워집니다.