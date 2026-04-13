# Plan: 일반 이메일 회원가입/로그인 및 소셜 계정 통합 (#200)

> 작성일: 2026-03-30
> 브랜치: `feat/200`
> 이슈: 일반 이메일 회원가입/로그인 및 소셜 계정 통합 (Member 스키마 확장)

---

## 1. 현황 분석

### 기존 인증 흐름

```
OAuth2 소셜 로그인
  → CustomOAuth2UserService.loadUser()
      - username = "{provider}_{socialId}" (예: "google_12345")
      - email 있으면 저장, 없으면 null
      - username 기준으로 Member 존재 여부 확인
      - 없으면 RegisterSocialMemberUseCase → 랜덤 닉네임으로 신규 생성
  → CustomOAuth2LoginSuccessHandler
      - TokenService.generateToken() → JWT 발급
      - 프론트로 accessToken, refreshToken 쿼리파라미터 redirect
```

### 현재 Member 엔티티 핵심 필드

| 필드 | 타입 | 제약 |
|------|------|------|
| `id` | Long | PK |
| `username` | String | UNIQUE, NOT NULL |
| `nickname` | String | UNIQUE, NOT NULL |
| `email` | String | Nullable |
| `role` | Role | NOT NULL |
| `point` | int | NOT NULL |

- `password` 없음, `authProvider` 없음
- `CustomUserDetailsService.loadUserByUsername()`은 memberId(Long)로 조회 (JWT 필터 전용)
- Flyway 미사용, Hibernate DDL auto 사용

---

## 2. 사전 점검 결과 (기구현 항목)

| 항목 | 파일 | 상태 |
|------|------|------|
| `MemberAdaptor.existsByNickname()` | `MemberAdaptor.java:77` | ✅ 이미 구현 — SignUpUseCase에서 그대로 사용 |
| `MemberErrorStatus.NICKNAME_ALREADY_EXISTS` | `MemberErrorStatus.java:17` | ✅ 이미 구현 — 에러 코드 신규 추가 불필요 |
| 그 외 AuthProvider, password 필드, email 조회, BCrypt, UseCase, Account Linking 등 | — | ❌ 미구현 → 전부 작업 필요 |

---

## 3. 변경 범위 요약

| 카테고리 | 파일 수 | 작업 유형 |
|----------|---------|-----------|
| 도메인 (enum, entity) | 2 | 신규 + 수정 |
| Repository / Adaptor | 2 | 수정 |
| Service | 1 | 수정 |
| UseCase | 2 | 신규 |
| Controller / DTO | 3 | 수정 |
| Security Config | 2 | 수정 + 신규 |
| OAuth2 Account Linking | 1 | 수정 |
| Error 코드 | 1 | 수정 |

---

## 4. 상세 구현 계획

### Step 1 — `AuthProvider` enum 신규 생성

**파일**: `member/domain/vo/AuthProvider.java`

```java
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    NAVER,
    KAKAO
}
```

- 기존 `SocialType` enum과 역할 구분:
  - `SocialType`: OAuth2 처리 중 provider 식별용 (common.auth 패키지)
  - `AuthProvider`: Member 엔티티의 가입 출처 기록용 (member 도메인)

---

### Step 2 — `Member` 엔티티 수정

**파일**: `member/domain/entity/Member.java`

추가 필드:

```java
@Column                                        // nullable = true 가 JPA 기본값 — 명시 불필요
private String password;                       // 소셜 유저는 null 허용

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private AuthProvider authProvider;             // LOCAL, GOOGLE, NAVER, KAKAO
```

추가 정적 팩토리 메서드:

```java
// 소셜 회원 생성 (기존 initializeMember 대체)
public static Member ofSocial(String username, String email, AuthProvider authProvider) {
    // username = "{provider}_{socialId}", authProvider = GOOGLE/NAVER/KAKAO
    // nickname = 랜덤 (기존 로직 유지)
}

// 일반 이메일 회원 생성
// username은 "local_{UUID}" 형태로 내부 생성 — 외부에서 주입 불필요
public static Member ofLocal(String email, String nickname, String encodedPassword) {
    String username = "local_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    // role = USER, authProvider = LOCAL
}
```

> **설계 근거**: LOCAL 회원의 `username`을 email로 쓰면 email 변경 시 UNIQUE 제약이 깨진다.
> `local_{uuid}` 내부 생성 방식으로 외부 의존을 차단한다.

---

### Step 3 — `MemberRepository` 수정

**파일**: `member/infrastructure/persistence/MemberRepository.java`

```java
Optional<Member> findByEmail(String email);

boolean existsByEmail(String email);
```

---

### Step 4 — `MemberAdaptor` 수정

**파일**: `member/application/adaptor/MemberAdaptor.java`

> `existsByNickname(String nickname)` 은 **이미 구현됨** (line 77) — 추가 불필요.

추가 메서드 (이메일 관련 2개만):

```java
// 이메일로 조회. 없으면 EMAIL_NOT_FOUND 예외 (MEMBER_NOT_FOUND 아님)
// LocalLoginUseCase가 catch 없이 바로 사용할 수 있도록 도메인 맞는 예외 사용
public Member queryByEmail(String email) {
    return memberRepository.findByEmail(email)
        .orElseThrow(() -> new MemberHandler(MemberErrorStatus.EMAIL_NOT_FOUND));
}

// 이메일 존재 여부 (SignUpUseCase 중복 체크용)
public boolean existsByEmail(String email) {
    return memberRepository.existsByEmail(email);
}
```

---

### Step 5 — `MemberService` 수정

**파일**: `member/application/service/MemberService.java`

기존 `initializeMember(username, email)` → `initializeSocialMember`로 rename:

```java
// 소셜 회원 초기화 (AuthProvider 추가)
public Member initializeSocialMember(String username, String email, AuthProvider authProvider) {
    // Member.ofSocial(username, email, authProvider) 호출
    // 기존 랜덤 닉네임 생성 로직 유지
}
```

신규 메서드:

```java
// 일반 이메일 회원 초기화
// encodedPassword: 호출 측(UseCase)에서 이미 BCrypt 인코딩된 값을 전달받음
public Member initializeLocalMember(String email, String nickname, String encodedPassword) {
    return memberRepository.save(Member.ofLocal(email, nickname, encodedPassword));
}
```

> `RegisterSocialMemberUseCase`의 `initializeMember` 호출도 `initializeSocialMember`로 함께 수정 (Step 12).

---

### Step 6 — `PasswordEncoderConfig` 신규 생성

**파일**: `common/security/config/PasswordEncoderConfig.java` ← **SecurityConfig와 분리**

```java
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> `SecurityConfig`에 넣으면 보안 설정 파일에 암호화 책임이 혼재된다 (SRP 위반).
> Spring Security 필터 체인 설정과 암호화 빈은 별도 파일로 관리한다.
> `AuthenticationManager` 빈 노출은 **불필요** — 로그인 검증을 UseCase에서 직접 처리하므로.

---

### Step 7 — `SignUpUseCase` 신규 생성

**파일**: `member/application/usecase/SignUpUseCase.java`
(**`auth/`가 아닌 `member/` 패키지** — 기존 `RegisterSocialMemberUseCase`와 동일 위치)

```
@UseCase
@Transactional
public class SignUpUseCase {
    execute(email, nickname, rawPassword):
        1. memberAdaptor.existsByEmail(email)
               → true면 MemberErrorStatus.EMAIL_ALREADY_EXISTS 예외
        2. memberAdaptor.existsByNickname(nickname)
               → true면 MemberErrorStatus.NICKNAME_ALREADY_EXISTS 예외 (기존 코드)
        3. encodedPassword = passwordEncoder.encode(rawPassword)
        4. member = memberService.initializeLocalMember(email, nickname, encodedPassword)
        5. 통계 초기화 (RegisterSocialMemberUseCase 참고)
        6. 게임 랭크 초기화
        7. 기본 마커 아이템 배정 및 장착
        8. Redis 프로필 캐시
        9. (선택) Slack 신규 가입 알림
        10. JwtToken token = tokenService.generateTokenByMember(member)  ← 토큰 즉시 발급
        11. return SignUpResult(member.getId(), token.accessToken(), token.refreshToken())
```

> 회원가입 후 토큰 즉시 발급 — 별도 로그인 단계를 요구하지 않는 것이 UX상 자연스럽다.

---

### Step 8 — `LocalLoginUseCase` 신규 생성

**파일**: `auth/application/usecase/LocalLoginUseCase.java`

```
@UseCase
@Transactional(readOnly = true)
public class LocalLoginUseCase {
    execute(email, rawPassword):
        1. member = memberAdaptor.queryByEmail(email)
               → 없으면 MemberErrorStatus.EMAIL_NOT_FOUND 예외 (Adaptor 내부에서 throw)

        2. member.getAuthProvider() != LOCAL  (즉, 소셜 전용 계정)
               → MemberErrorStatus.SOCIAL_ACCOUNT_ONLY 예외
               ※ password가 null인 상태에서 passwordEncoder.matches() 호출 시 NPE 발생
                  → 반드시 이 체크가 password 검증보다 먼저 와야 한다

        3. !passwordEncoder.matches(rawPassword, member.getPassword())
               → MemberErrorStatus.INVALID_PASSWORD 예외

        4. JwtToken token = tokenService.generateTokenByMember(member)
        5. return LoginResult(token.accessToken(), token.refreshToken())
}
```

#### `TokenService`에 헬퍼 메서드 추가

현재 `generateToken(Authentication)`은 `Authentication` 객체를 요구한다.
LocalLogin은 `AuthenticationManager`를 거치지 않으므로 Member로 직접 `Authentication` 구성:

```java
public JwtToken generateTokenByMember(Member member) {
    CustomUserDetails userDetails = CustomUserDetails.from(member);
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities()
    );
    return generateToken(authentication);  // 기존 generateToken 재사용
}
```

> `CustomUserDetails.getPassword()`는 현재 `null`을 반환한다 (OAuth2 전용 설계).
> LOCAL 회원도 JWT 발급 이후에는 동일 필터 체인을 타므로 변경 불필요.

---

### Step 9 — `AuthController` 수정

**파일**: `auth/presentation/controller/AuthController.java`

엔드포인트 추가:

```java
POST /auth/signup
  body: AuthRequest.SignUp
  → signUpUseCase.execute()
  → return ApiResponseDto.onSuccess(AuthResponse.SignUpResult)

POST /auth/login
  body: AuthRequest.LocalLogin
  → localLoginUseCase.execute()
  → return ApiResponseDto.onSuccess(AuthResponse.LoginResult)
```

**DTO** (`AuthRequest.java` / `AuthResponse.java` 내부 클래스로 추가):

```java
// AuthRequest 추가
public static class SignUp {
    @NotBlank @Email                          // @Email 형식 검증 필수
    private String email;
    @NotBlank @Size(min = 8)
    private String password;
    @NotBlank @Size(min = 2, max = 12)
    private String nickname;
}

public static class LocalLogin {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}

// AuthResponse 추가
public static class SignUpResult {
    private Long memberId;
    private String accessToken;
    private String refreshToken;

    public static SignUpResult from(Long memberId, JwtToken token) { ... }
}

public static class LoginResult {
    private String accessToken;
    private String refreshToken;

    public static LoginResult from(JwtToken token) { ... }
}
```

> 기존 `AuthResponse.TempLogin`의 `from(JwtToken, memberId)` 패턴을 그대로 따른다.

---

### Step 10 — SecurityConfig 수정 (공개 엔드포인트 추가)

**파일**: `common/security/config/SecurityConfig.java`

기존 `permitAll()` 블록에 추가:

```java
.requestMatchers("/auth/signup", "/auth/login").permitAll()
```

---

### Step 11 — 소셜 계정 연동 (Account Linking)

**파일**: `common/auth/service/CustomOAuth2UserService.java`

변경 로직:

```java
// 1. username({provider}_{id}) 기준 조회 — 기존 소셜 유저
Optional<Member> byUsername = memberRepository.findByUsername(username);
if (byUsername.isPresent()) {
    return new CustomOAuthUser(byUsername.get(), authorities, oAuth2User.getAttributes());
}

// 2. email 기준 조회 — 동일 이메일로 이미 가입된 계정 (LOCAL 또는 다른 소셜)
if (email != null) {
    Optional<Member> byEmail = memberRepository.findByEmail(email);
    if (byEmail.isPresent()) {
        // 기존 계정으로 로그인 처리 (신규 생성 없음)
        // username 컬럼은 변경하지 않음 — 단일 username 스키마 한계
        return new CustomOAuthUser(byEmail.get(), authorities, oAuth2User.getAttributes());
    }
}

// 3. 신규 소셜 가입
Member newMember = registerSocialMemberUseCase.execute(username, email, authProvider);
                    // ↑ execute()가 Member를 반환하도록 Step 12에서 수정
return new CustomOAuthUser(newMember, authorities, oAuth2User.getAttributes());
```

> **스키마 한계**: `username` 필드가 단일 컬럼이므로 LOCAL+소셜 연동 후 소셜 username을 별도 저장 불가.
> MVP에서는 "email 일치 시 기존 계정으로 로그인"으로 처리하고, 다중 소셜 연동이 필요하면
> 별도 `member_social_account` 연결 테이블을 추가한다.

---

### Step 12 — `RegisterSocialMemberUseCase` 수정

**파일**: `member/application/usecase/RegisterSocialMemberUseCase.java`

변경:
- `execute(String username, String email)` → `execute(String username, String email, AuthProvider authProvider)`
- 반환 타입: `void` → `Member` (Step 11의 `newMember` 참조를 위해)
- 내부: `memberService.initializeSocialMember(username, email, authProvider)` 호출

`CustomOAuth2UserService`에서 `authProvider` 전달:

```java
AuthProvider authProvider = switch (socialType) {
    case GOOGLE -> AuthProvider.GOOGLE;
    case NAVER  -> AuthProvider.NAVER;
    case KAKAO  -> AuthProvider.KAKAO;
};
```

---

### Step 13 — 에러 코드 추가

**파일**: `member/domain/exception/MemberErrorStatus.java`

> `NICKNAME_ALREADY_EXISTS` (4101) 는 **이미 구현됨** — 추가 불필요.

추가 (4102~4105 범위):

```java
EMAIL_ALREADY_EXISTS(CONFLICT, 4102, "이미 사용 중인 이메일입니다."),
EMAIL_NOT_FOUND(UNAUTHORIZED, 4103, "존재하지 않는 이메일입니다."),
    // ※ NOT_FOUND(404) 대신 UNAUTHORIZED(401) 사용 — 이메일 존재 여부 노출 방지 (보안)
INVALID_PASSWORD(UNAUTHORIZED, 4104, "비밀번호가 일치하지 않습니다."),
SOCIAL_ACCOUNT_ONLY(BAD_REQUEST, 4105, "소셜 계정 전용입니다. 소셜 로그인을 이용해주세요."),
```

> 이메일/비밀번호는 Member 도메인 관련이므로 `MemberErrorStatus`에 위치.
> `ErrorStatus`(공통)에는 추가하지 않음 — 4050~4057 auth 코드와 도메인 혼재 방지.

---

## 5. 구현 순서 (의존 관계 기반)

```
1.  AuthProvider enum 신규 생성
2.  Member 엔티티 수정 (password, authProvider 필드 + ofSocial/ofLocal 팩토리)
3.  MemberRepository 수정 (findByEmail, existsByEmail)
4.  MemberAdaptor 수정 (queryByEmail → EMAIL_NOT_FOUND, existsByEmail)
5.  MemberErrorStatus 수정 (EMAIL_ALREADY_EXISTS 등 4개 추가)
6.  MemberService 수정 (initializeSocialMember rename, initializeLocalMember 추가)
7.  PasswordEncoderConfig 신규 생성 (BCryptPasswordEncoder 빈)
8.  TokenService 수정 (generateTokenByMember 헬퍼 추가)
9.  SignUpUseCase 신규 생성 (member/application/usecase/)
10. LocalLoginUseCase 신규 생성 (auth/application/usecase/)
11. AuthRequest / AuthResponse DTO 수정 (SignUp, LocalLogin, SignUpResult, LoginResult)
12. AuthController 수정 (signup, login 엔드포인트)
13. SecurityConfig 수정 (공개 엔드포인트 추가)
14. RegisterSocialMemberUseCase 수정 (authProvider 전달, Member 반환)
15. CustomOAuth2UserService 수정 (account linking 로직)
```

---

## 6. 스키마 변경 (DDL)

Flyway 미사용이므로 Hibernate `ddl-auto: update`로 컬럼이 자동 추가된다.
단, 기존 소셜 유저 데이터에 `auth_provider NOT NULL` 제약이 걸리면 마이그레이션 이슈 발생.

**처리 방안**:

```sql
-- 운영 배포 시 수동 DDL:
ALTER TABLE member ADD COLUMN password VARCHAR(255) NULL;
ALTER TABLE member ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'GOOGLE';
-- 이후 실제 provider별 데이터 보정 필요 (naver/kakao 유저 구분)
```

> 이번 기회에 `resources/db/migration/` 하위에 Flyway 도입을 권장.
> 현재는 `ddl-auto: update`로 로컬/테스트 환경에서 개발 진행.

---

## 7. 테스트 계획

| 테스트 케이스 | 유형 | 기대 결과 |
|--------------|------|-----------|
| 일반 회원가입 성공 | UseCase | JWT 즉시 반환 |
| 이메일 중복 회원가입 | UseCase | `EMAIL_ALREADY_EXISTS` 예외 |
| 닉네임 중복 회원가입 | UseCase | `NICKNAME_ALREADY_EXISTS` 예외 (기존 코드) |
| 일반 로그인 성공 | UseCase | JWT 반환 |
| 소셜 전용 계정 일반 로그인 시도 | UseCase | `SOCIAL_ACCOUNT_ONLY` 예외 (password 검증 전 체크) |
| 틀린 비밀번호 로그인 | UseCase | `INVALID_PASSWORD` 예외 |
| 존재하지 않는 이메일 로그인 | UseCase | `EMAIL_NOT_FOUND` 예외 (401 반환) |
| 소셜 로그인 — 신규 계정 | UseCase | authProvider 올바르게 저장 |
| 소셜 로그인 — 기존 이메일 계정 연동 | UseCase | 기존 Member 반환, 신규 생성 없음 |

---

## 8. 고려 사항 및 결정 포인트

### 8-1. LOCAL 회원의 `username` 처리
- `ofLocal()` 팩토리 내부에서 `local_{uuid8}` 형태로 자동 생성
- email을 username으로 쓰면 email 변경 시 UNIQUE 충돌 발생 → 내부 uuid 방식이 안전

### 8-2. 소셜 로그인 기존 계정 연동 시 `username` 처리
- LOCAL 계정의 username(`local_{uuid}`)은 소셜 로그인 후에도 변경하지 않음
- 소셜 `{provider}_{socialId}` 저장 필요 시 향후 `member_social_account` 연결 테이블 도입

### 8-3. password 유효성 검사 정책
- `@Size(min = 8)` 최소 길이만 적용 (이슈 명세 기준)
- 영문+숫자 조합 강제가 필요하면 `@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$")` 추가

### 8-4. 이메일 인증
- 이슈 명세에 없으므로 MVP 범위 외 — 제외

### 8-5. `RegisterSocialMemberUseCase.execute()` 반환 타입 변경
- 기존 `void` → `Member` 반환으로 변경 시 호출부(`CustomOAuth2UserService`)가 영향을 받음
- 변경 범위가 크지 않으므로 함께 수정

---

## 9. 파일 목록 (신규/수정)

### 신규 파일
- `member/domain/vo/AuthProvider.java`
- `member/application/usecase/SignUpUseCase.java`
- `auth/application/usecase/LocalLoginUseCase.java`
- `common/security/config/PasswordEncoderConfig.java`

### 수정 파일
- `member/domain/entity/Member.java` — password, authProvider 추가; ofSocial/ofLocal 팩토리
- `member/domain/exception/MemberErrorStatus.java` — 에러 코드 4개 추가
- `member/infrastructure/persistence/MemberRepository.java` — findByEmail, existsByEmail
- `member/application/adaptor/MemberAdaptor.java` — queryByEmail(EMAIL_NOT_FOUND), existsByEmail
- `member/application/service/MemberService.java` — initializeSocialMember rename, initializeLocalMember 추가
- `member/application/usecase/RegisterSocialMemberUseCase.java` — authProvider 전달, Member 반환
- `common/security/config/SecurityConfig.java` — 공개 엔드포인트 추가
- `common/security/service/TokenService.java` — generateTokenByMember 추가
- `common/auth/service/CustomOAuth2UserService.java` — account linking 로직
- `auth/presentation/controller/AuthController.java` — signup, login 엔드포인트
- `auth/presentation/dto/request/AuthRequest.java` — SignUp, LocalLogin inner class 추가
- `auth/presentation/dto/response/AuthResponse.java` — SignUpResult, LoginResult inner class 추가
