# Plan: 이메일 기반 비밀번호 재설정 구현 (Issue #202)

## 현황 파악

### 이미 갖춰진 것
- `Member.ofLocal()` / `AuthProvider.LOCAL` — 로컬 계정 구분 가능
- `BCryptPasswordEncoder` — `PasswordEncoderConfig`에서 Bean으로 등록됨
- `RedisTemplate<String, Object>` — 기존 Redis 인프라 존재 (토큰용은 별도 `StringRedisTemplate` 사용)
- `spring-boot-starter-thymeleaf` — 이메일 HTML 템플릿 렌더링에 바로 사용 가능
- `MemberAdaptor.queryByEmail()` / `existsByEmail()` — 이메일 조회 메서드 존재
- `MemberErrorStatus` — 4106번부터 추가 가능 (현재 4105까지 사용)
- `GeneralException` + `ExceptionAdvice` — 커스텀 예외 처리 인프라 완비
- `@Async` — 이미 프로젝트에서 게임 통계 / 알림에 사용 중
- `SlackNotifier` — 서버 에러 시 Slack 알림 가능

### 추가가 필요한 것
- `spring-boot-starter-mail` 의존성
- `EmailErrorStatus` + `EmailHandler` — 커스텀 이메일 예외
- `Member.updatePassword()` / `MemberService.updatePassword()` — 비밀번호 변경
- `MemberAdaptor.findByEmail()` — Optional 반환 (기존 `queryByEmail`은 예외 throw)
- `MemberErrorStatus.PASSWORD_RESET_TOKEN_INVALID` — 에러 코드 1개
- Redis 토큰 저장소 (Repository → infrastructure, Service → application)
- `EmailService` — 범용 HTML 메일 발송 (도메인 비종속)
- `PasswordResetEmailComposer` — 비밀번호 재설정 전용 템플릿 조합
- `PasswordResetProperties` — 외부화 설정
- UseCase 2개 (Request / Confirm)
- Rate Limit Redis 저장소
- SecurityConfig `authRelatedEndpoints()` URL 추가
- `application-test.yml` mail 더미 설정

---

## 아키텍처 결정

### Redis 키 설계

```
password-reset:{UUID_TOKEN}       →  {memberId}   TTL: 15분  (토큰 저장)
password-reset-rate:{email}       →  {count}      TTL: 1시간 (요청 횟수 제한)
```

- 토큰은 단순 String 값으로 저장 (`StringRedisTemplate` 활용)
- `StringRedisTemplate`을 별도 사용하는 이유: 기존 `RedisTemplate<String, Object>`의 직렬화 설정과 충돌 방지
- **`getAndDelete()`** (Redis `GETDEL`) — 토큰 조회 + 삭제를 원자적으로 처리, 레이스 컨디션 방지
- TTL 만료 = 자동 무효화 / 사용 후 `getAndDelete` = 단일 사용 보장

### 레이어 설계 (DDD 준수)

```
PasswordResetTokenRedisRepository  →  auth/infrastructure/redis/   (순수 I/O)
PasswordResetTokenRedisService     →  auth/application/service/    (비즈니스 로직: UUID 생성, 예외 throw)
PasswordResetRateLimitService      →  auth/application/service/    (Rate Limit 제어)
```

- Infrastructure 레이어에 비즈니스 로직(UUID 생성, 예외 판단)을 두지 않는다.
- UseCase는 application 레이어의 Service/Adaptor만 orchestrate한다.

### 이메일 발송 설계 (관심사 분리)

```
EmailService                  →  common/email/         (범용: sendHtmlEmail(to, subject, html))
PasswordResetEmailComposer    →  auth/application/     (도메인 특화: 템플릿 + 링크 조합)
```

- `EmailService`는 도메인을 모른다. HTML 문자열만 받아서 발송.
- `PasswordResetEmailComposer`가 Thymeleaf로 HTML을 만들고 링크를 조합.
- 나중에 회원가입 환영 메일 등 다른 이메일이 추가될 때 `EmailService`를 재사용 가능.

### 이메일 발송 동기 처리

- **동기** 처리를 선택한다.
- 이유: 메일 발송 실패 시 사용자에게 즉시 에러를 전달해야 재시도가 가능하다.
- 비동기(`@Async`)로 처리하면 발송 실패가 silent하게 묻혀 사용자가 링크를 못 받은 이유를 알 수 없다.
- SMTP 응답 지연(~2초)은 비밀번호 찾기 UX에서 허용 가능하다.

### 보안 설계

- **이메일 존재 여부 노출 금지**: 요청 API는 항상 200 반환 (이메일 열거 공격 방지)
- **토큰 단일 사용**: `getAndDelete`로 조회와 삭제 원자적 처리
- **LOCAL 계정 전용**: 소셜 계정 이메일 요청 시 no-op
- **Rate Limiting**: 동일 이메일로 1시간 내 5회 초과 요청 시 차단
- **비밀번호 검증**: `@Size(min=8)` + `@NotBlank` (기존 SignUp 수준 일관성 유지)

---

## 구현 순서 (의존 관계 순)

```
1.  build.gradle                   — mail 의존성 추가
2.  application.yml                — mail + app 프로퍼티 추가
3.  application-local.yml          — 로컬 테스트용 Mailhog 설정
4.  application-test.yml           — 테스트용 mail 더미 설정
5.  ErrorStatus                    — EMAIL_SEND_FAILED 추가
6.  EmailHandler                   — GeneralException 상속 커스텀 예외
7.  MemberErrorStatus              — PASSWORD_RESET_TOKEN_INVALID 추가
8.  Member (domain)                — updatePassword() 메서드 추가
9.  MemberService                  — updatePassword() 메서드 추가
10. MemberAdaptor                  — findByEmail(Optional) 메서드 추가
11. PasswordResetProperties        — @ConfigurationProperties (Service보다 먼저)
12. PasswordResetTokenRedisRepository  — infrastructure, 순수 I/O
13. PasswordResetTokenRedisService     — application/service, 비즈니스 로직
14. PasswordResetRateLimitService      — application/service, Rate Limit
15. EmailService                   — common/email, 범용 HTML 메일 발송
16. PasswordResetEmailComposer     — auth/application, 템플릿 조합
17. Thymeleaf 템플릿               — password-reset.html
18. RequestPasswordResetUseCase
19. ConfirmPasswordResetUseCase
20. AuthRequest                    — inner DTO 2개 추가
21. AuthController                 — 엔드포인트 2개 추가
22. SecurityConfig                 — authRelatedEndpoints() URL 추가
```

---

## 파일별 구현 명세

### 1. `build.gradle`
```gradle
implementation 'org.springframework.boot:spring-boot-starter-mail'
```

---

### 2. `application.yml`
```yaml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

app:
  password-reset:
    base-url: ${PASSWORD_RESET_BASE_URL:https://kospot.kr/reset-password}
    from-email: ${MAIL_FROM:noreply@kospot.kr}
    token-ttl-minutes: 15
    rate-limit-max: 5
    rate-limit-ttl-hours: 1
```

---

### 3. `application-local.yml`
```yaml
spring:
  mail:
    host: localhost
    port: 1025        # Mailhog SMTP
    username: test
    password: test
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false

app:
  password-reset:
    base-url: http://localhost:3000/reset-password
    from-email: noreply@kospot.local
```
> 로컬: `docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog`
> 수신 확인: `http://localhost:8025`

---

### 4. `application-test.yml`
```yaml
spring:
  mail:
    host: localhost
    port: 3025        # 테스트에서 실제 발송하지 않음 — MockBean으로 대체
    username: test
    password: test
```
> 통합 테스트에서는 `@MockBean JavaMailSender`로 실제 발송 차단.

---

### 5. `ErrorStatus.java` (이메일 에러 추가)
```java
// Email Error (4411 ~ 4420)
EMAIL_SEND_FAILED(INTERNAL_SERVER_ERROR, 4411, "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."),
EMAIL_RATE_LIMIT_EXCEEDED(TOO_MANY_REQUESTS, 4412, "이메일 요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
```

---

### 6. `EmailHandler.java`
```
위치: common/exception/object/domain/EmailHandler.java
```
```java
public class EmailHandler extends GeneralException {
    public EmailHandler(BaseCode code) {
        super(code);
    }
}
```
- 기존 `S3Handler`, `GameHandler` 등과 동일한 패턴

---

### 7. `MemberErrorStatus.java`
```java
PASSWORD_RESET_TOKEN_INVALID(BAD_REQUEST, 4106, "유효하지 않거나 만료된 비밀번호 재설정 토큰입니다.");
```
- 만료 / 미존재 / 이미 사용된 토큰 → 단일 에러로 통합 (토큰 유형 노출 방지)

---

### 8. `Member.java`
```java
public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
}
```

---

### 9. `MemberService.java`
```java
// memberId로 직접 조회 + 업데이트 — UseCase에서 Member를 별도 조회하지 않아도 됨
// ConfirmPasswordResetUseCase가 @Transactional 없이 이 메서드만 호출하면
// MemberService 클래스 레벨 @Transactional 안에서 조회+변경이 하나의 트랜잭션으로 처리됨
public void updatePasswordById(Long memberId, String encodedPassword) {
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND));
    member.updatePassword(encodedPassword);
}
```

---

### 10. `MemberAdaptor.java` (메서드 추가)
```java
public Optional<Member> findByEmail(String email) {
    return repository.findByEmail(email);
}
```
- `queryByEmail()`은 없으면 예외를 throw하지만, 비밀번호 재설정 요청에서는 이메일 미존재 시 no-op이므로 Optional 반환이 필요하다.
- 기존 `queryByEmail()`을 중복 호출(`existsByEmail` + `queryByEmail`)하지 않고 쿼리 1회로 줄인다.

---

### 11. `PasswordResetProperties.java`
```
위치: common/config/PasswordResetProperties.java
```
```java
@Getter
@Setter
@ConfigurationProperties(prefix = "app.password-reset")
public class PasswordResetProperties {
    private String baseUrl;
    private String fromEmail;
    private long tokenTtlMinutes;
    private int rateLimitMax;
    private long rateLimitTtlHours;
}
```
- `@Component` 없이 `@ConfigurationProperties`만 선언
- `@EnableConfigurationProperties(PasswordResetProperties.class)`를 공통 Config 클래스(또는 별도 `PasswordResetConfig`)에 선언하여 Bean 등록

---

### 12. `PasswordResetTokenRedisRepository.java`
```
위치: auth/infrastructure/redis/PasswordResetTokenRedisRepository.java
```
```java
@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRedisRepository {

    private static final String TOKEN_KEY_PREFIX = "password-reset:";
    private static final String RATE_KEY_PREFIX  = "password-reset-rate:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveToken(String token, Long memberId, long ttlMinutes) {
        stringRedisTemplate.opsForValue()
            .set(TOKEN_KEY_PREFIX + token, String.valueOf(memberId), ttlMinutes, TimeUnit.MINUTES);
    }

    // GETDEL: 조회+삭제 원자적 처리 (Spring Data Redis 3.x / Redis 6.2+)
    public Optional<Long> getAndDeleteToken(String token) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + token);
        if (value == null) return Optional.empty();
        return Optional.of(Long.parseLong(value));
    }

    // Rate Limit: increment + TTL 설정 (최초 요청 시에만 TTL 세팅)
    public long incrementRateLimit(String email, long ttlHours) {
        String key = RATE_KEY_PREFIX + email;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
        }
        return count == null ? 0L : count;
    }
}
```
> Rate Limit에서 `increment`와 `expire`가 원자적이지 않은 문제:
> count == 1일 때만 TTL 설정하므로, 최초 set 후 expire 사이에 서버가 죽는 극단적 케이스에서 키가 TTL 없이 남을 수 있다.
> 허용 가능한 트레이드오프 (비밀번호 재설정 수준에서 Lua Script까지는 과도).

---

### 13. `PasswordResetTokenRedisService.java`
```
위치: auth/application/service/PasswordResetTokenRedisService.java
```
```java
@Service
@RequiredArgsConstructor
public class PasswordResetTokenRedisService {

    private final PasswordResetTokenRedisRepository repository;
    private final PasswordResetProperties properties;

    public String generateAndSave(Long memberId) {
        String token = UUID.randomUUID().toString();
        repository.saveToken(token, memberId, properties.getTokenTtlMinutes());
        return token;
    }

    public Long getAndInvalidate(String token) {
        return repository.getAndDeleteToken(token)
            .orElseThrow(() -> new MemberHandler(MemberErrorStatus.PASSWORD_RESET_TOKEN_INVALID));
    }
}
```

---

### 14. `PasswordResetRateLimitService.java`
```
위치: auth/application/service/PasswordResetRateLimitService.java
```
```java
@Service
@RequiredArgsConstructor
public class PasswordResetRateLimitService {

    private final PasswordResetTokenRedisRepository repository;
    private final PasswordResetProperties properties;

    public void checkAndIncrement(String email) {
        long count = repository.incrementRateLimit(email, properties.getRateLimitTtlHours());
        if (count > properties.getRateLimitMax()) {
            throw new EmailHandler(ErrorStatus.EMAIL_RATE_LIMIT_EXCEEDED);
        }
    }
}
```

---

### 15. `EmailService.java`
```
위치: common/email/EmailService.java
```
```java
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendHtmlEmail(String to, String subject, String fromEmail, String htmlContent) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(fromEmail);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailHandler(ErrorStatus.EMAIL_SEND_FAILED);
        }
    }
}
```
- `EmailService`는 도메인을 전혀 모른다. HTML 문자열만 받아서 발송.
- `MimeMessageHelper(message, false, "UTF-8")`: 첨부파일 없는 HTML 메일은 multipart 불필요.
- `MessagingException` → `EmailHandler`로 변환하여 `ExceptionAdvice`에서 일관되게 처리.

---

### 16. `PasswordResetEmailComposer.java`
```
위치: auth/application/service/PasswordResetEmailComposer.java
```
```java
@Service
@RequiredArgsConstructor
public class PasswordResetEmailComposer {

    private static final String TEMPLATE_NAME = "email/password-reset";
    private static final String SUBJECT = "[KoSpot] 비밀번호 재설정 안내";

    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final PasswordResetProperties properties;

    public void send(String toEmail, String resetToken) {
        String resetLink = properties.getBaseUrl() + "?token=" + resetToken;

        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        String html = templateEngine.process(TEMPLATE_NAME, context);

        emailService.sendHtmlEmail(toEmail, SUBJECT, properties.getFromEmail(), html);
    }
}
```

---

### 17. `templates/email/password-reset.html`
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head><meta charset="UTF-8"/></head>
<body>
  <h2>KoSpot 비밀번호 재설정</h2>
  <p>아래 버튼을 클릭하여 비밀번호를 재설정하세요. 링크는 15분간 유효합니다.</p>
  <a th:href="${resetLink}"
     style="display:inline-block;padding:12px 24px;background:#4F46E5;color:#fff;
            text-decoration:none;border-radius:6px;">
    비밀번호 재설정
  </a>
  <p style="color:#999;font-size:12px;">
    본인이 요청하지 않은 경우 이 메일을 무시하세요.
  </p>
</body>
</html>
```

---

### 18. `RequestPasswordResetUseCase.java`
```
위치: auth/application/usecase/RequestPasswordResetUseCase.java
```
```java
@UseCase
@RequiredArgsConstructor
// @Transactional 없음 — Redis 작업과 DB 작업이 혼재하므로 클래스 레벨 트랜잭션 선언 금지
// DB 읽기는 MemberAdaptor의 @Transactional(readOnly = true)가 처리
public class RequestPasswordResetUseCase {

    private final MemberAdaptor memberAdaptor;
    private final PasswordResetTokenRedisService tokenRedisService;
    private final PasswordResetRateLimitService rateLimitService;
    private final PasswordResetEmailComposer emailComposer;

    public void execute(String email) {
        // 1. Rate Limit 체크 (Redis) — 트랜잭션 없음
        rateLimitService.checkAndIncrement(email);

        // 2. DB 조회 — MemberAdaptor 자체 @Transactional(readOnly = true) 적용
        Optional<Member> memberOpt = memberAdaptor.findByEmail(email);
        if (memberOpt.isEmpty()) return;

        Member member = memberOpt.get();
        if (member.getAuthProvider() != AuthProvider.LOCAL) return;

        // 3. 토큰 저장 (Redis) — 트랜잭션 없음
        String token = tokenRedisService.generateAndSave(member.getId());

        // 4. 이메일 발송 — 트랜잭션 없음
        emailComposer.send(email, token);
    }
}
```

**실행 순서와 실패 시나리오**:
- 각 단계는 독립적으로 실행, Spring 트랜잭션으로 묶이지 않음
- 3번(Redis 토큰 저장) 후 4번(이메일) 실패 시 → Redis 토큰은 남아있으나 15분 TTL로 자연 만료 (허용 가능)

---

### 19. `ConfirmPasswordResetUseCase.java`
```
위치: auth/application/usecase/ConfirmPasswordResetUseCase.java
```
```java
@UseCase
@RequiredArgsConstructor
// @Transactional 없음 — Redis 삭제(getAndInvalidate)를 DB 트랜잭션 밖에서 먼저 수행해야 함
// DB 쓰기는 MemberService.updatePasswordById()의 @Transactional이 처리
public class ConfirmPasswordResetUseCase {

    private final PasswordResetTokenRedisService tokenRedisService;
    private final MemberService memberService;
    private final BCryptPasswordEncoder passwordEncoder;

    public void execute(String token, String newPassword) {
        // 1. Redis 토큰 조회 + 삭제 (GETDEL, 원자적) — 트랜잭션 없음
        //    @Transactional 안에서 호출 시 DB rollback과 무관하게 Redis 삭제가 커밋되어
        //    토큰 유실 버그 발생 → 반드시 트랜잭션 외부에서 실행
        Long memberId = tokenRedisService.getAndInvalidate(token);

        // 2. DB 조회 + 비밀번호 업데이트 — MemberService @Transactional 안에서 하나의 트랜잭션으로 처리
        memberService.updatePasswordById(memberId, passwordEncoder.encode(newPassword));
    }
}
```

**설계 의도**:
- Redis 삭제를 DB 트랜잭션 경계 바깥에 두어, DB 실패 시 토큰이 유실되는 버그를 원천 차단
- `MemberAdaptor` 주입 불필요 — `updatePasswordById`가 내부에서 조회까지 담당

---

### 20. `AuthRequest.java` (inner DTO 추가)
```java
@Data @AllArgsConstructor @NoArgsConstructor
public static class PasswordResetRequest {
    @NotBlank @Email
    private String email;
}

@Data @AllArgsConstructor @NoArgsConstructor
public static class ConfirmPasswordReset {
    @NotBlank
    private String token;
    @NotBlank @Size(min = 8)
    private String newPassword;
}
```

---

### 21. `AuthController.java` (엔드포인트 추가)
```java
private final RequestPasswordResetUseCase requestPasswordResetUseCase;
private final ConfirmPasswordResetUseCase confirmPasswordResetUseCase;

@Operation(summary = "비밀번호 재설정 메일 발송", description = "이메일로 비밀번호 재설정 링크 발송 (항상 200 반환)")
@PostMapping("/password-reset/request")
public ApiResponseDto<?> requestPasswordReset(
        @RequestBody @Valid AuthRequest.PasswordResetRequest request) {
    requestPasswordResetUseCase.execute(request.getEmail());
    return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
}

@Operation(summary = "비밀번호 재설정 확인", description = "토큰 검증 후 비밀번호 변경")
@PostMapping("/password-reset/confirm")
public ApiResponseDto<?> confirmPasswordReset(
        @RequestBody @Valid AuthRequest.ConfirmPasswordReset request) {
    confirmPasswordResetUseCase.execute(request.getToken(), request.getNewPassword());
    return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
}
```

---

### 22. `SecurityConfig.java`
```java
private RequestMatcher[] authRelatedEndpoints() {
    List<RequestMatcher> requestMatchers = List.of(
            antMatcher("/tokens/**"),
            antMatcher("/auth/signup"),
            antMatcher("/auth/login"),
            antMatcher("/auth/password-reset/**")   // 추가
    );
    return requestMatchers.toArray(RequestMatcher[]::new);
}
```

---

## 최종 디렉토리 구조

```
src/main/java/com/kospot/
├── auth/
│   ├── application/
│   │   ├── usecase/
│   │   │   ├── RequestPasswordResetUseCase.java       [NEW]
│   │   │   └── ConfirmPasswordResetUseCase.java       [NEW]
│   │   └── service/
│   │       ├── PasswordResetTokenRedisService.java    [NEW]  ← application 레이어
│   │       ├── PasswordResetRateLimitService.java     [NEW]
│   │       └── PasswordResetEmailComposer.java        [NEW]
│   ├── infrastructure/redis/
│   │   └── PasswordResetTokenRedisRepository.java     [NEW]  ← infrastructure 레이어
│   └── presentation/
│       ├── controller/AuthController.java              [MODIFY]
│       └── dto/request/AuthRequest.java                [MODIFY]
│
├── member/
│   ├── domain/
│   │   ├── entity/Member.java                         [MODIFY - updatePassword() 추가]
│   │   └── exception/MemberErrorStatus.java           [MODIFY - 에러 코드 1개 추가]
│   └── application/
│       ├── adaptor/MemberAdaptor.java                  [MODIFY - findByEmail() 추가]
│       └── service/MemberService.java                  [MODIFY - updatePassword() 추가]
│
└── common/
    ├── config/
    │   └── PasswordResetProperties.java                [NEW]
    ├── email/
    │   └── EmailService.java                           [NEW]  ← 범용, 도메인 비종속
    ├── exception/
    │   ├── object/domain/EmailHandler.java             [NEW]
    │   └── payload/code/ErrorStatus.java               [MODIFY - EMAIL_* 에러 2개 추가]
    └── security/config/SecurityConfig.java             [MODIFY]

src/main/resources/
├── templates/email/
│   └── password-reset.html                            [NEW]
├── application.yml                                     [MODIFY]
├── application-local.yml                              [MODIFY]
└── application-test.yml                               [MODIFY - mail 더미 설정]

build.gradle                                            [MODIFY]
```

---

## 주의사항 및 트레이드오프

### Rate Limit에서 increment + expire 비원자성
Redis `INCR` 후 `EXPIRE`를 따로 호출하므로, count가 1일 때 서버가 죽으면 TTL 없는 키가 남는다.
Lua Script로 원자적 처리 가능하나, 비밀번호 재설정 기능에서는 과도한 복잡도. 현재 방식 유지.

### UseCase에 `@Transactional` 선언 금지
두 UseCase 모두 클래스 레벨 `@Transactional`을 선언하지 않는다.

Redis는 Spring `@Transactional`에 참여하지 않는다. UseCase에 `@Transactional`을 선언하면 Redis 작업이 DB 트랜잭션 경계 안에 포함된 것처럼 보이는 착각을 유발하고, DB rollback 시 Redis 작업이 함께 되돌려진다는 오해를 낳는다.

**역할 분리**:
- Redis 작업 → 각 Redis Service가 트랜잭션 없이 직접 처리
- DB 읽기 → `MemberAdaptor`의 `@Transactional(readOnly = true)` 적용
- DB 쓰기 → `MemberService`의 `@Transactional` 적용

`RequestPasswordResetUseCase` 실행 순서 (트랜잭션 경계 없음):
1. Rate Limit 증가 (Redis)
2. 이메일 조회 (DB — MemberAdaptor 자체 트랜잭션)
3. 토큰 저장 (Redis)
4. 이메일 발송 (SMTP)

4번 실패 시 1·3번 Redis 작업은 이미 완료 상태. 이메일 발송 실패 → `EmailHandler` 예외 → 사용자 에러 안내 → 재시도 시 새 토큰 발급.

### 동일 이메일 중복 요청
기존 토큰을 명시적으로 무효화하지 않는다. 새 요청마다 새 토큰이 발급되며, 마지막으로 받은 링크만 실제 사용 가능하다. Rate Limit으로 무한 발급을 제한.

---

## 테스트 시나리오

| # | 시나리오 | 기대 결과 |
|---|---------|----------|
| 1 | 정상 플로우 (요청 → 메일 수신 → confirm) | 비밀번호 변경, Redis 토큰 삭제 확인 |
| 2 | 존재하지 않는 이메일로 요청 | 200 (no-op, Redis 토큰 저장 없음) |
| 3 | 소셜 계정 이메일로 요청 | 200 (no-op) |
| 4 | 만료된 토큰으로 confirm | 4106 에러 |
| 5 | 사용 완료된 토큰 재사용 | 4106 에러 |
| 6 | 존재하지 않는 토큰 | 4106 에러 |
| 7 | 비밀번호 8자 미만으로 confirm | 400 Validation 에러 |
| 8 | 변경된 비밀번호로 로그인 | 정상 JWT 발급 |
| 9 | 이전 비밀번호로 로그인 시도 | 4104 INVALID_PASSWORD |
| 10 | 동일 이메일로 1시간 내 6회 요청 | 4412 EMAIL_RATE_LIMIT_EXCEEDED |
| 11 | SMTP 발송 실패 시 | 4411 EMAIL_SEND_FAILED |
