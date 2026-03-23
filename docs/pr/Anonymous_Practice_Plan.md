# 비로그인 사용자 RoadView Practice 허용 — 상세 구현 플랜 (v3)

> **이슈**: feat/195
> **작성일**: 2026-03-22
> **목표**: 비로그인 사용자도 RoadView Practice start / end / reissue 가능하도록 하되,
> 로그인 사용자의 기존 흐름(통계/포인트/기록)은 완전히 유지하고
> 익명 플레이가 다른 사용자의 게임에 영향을 주지 않도록 보안 장치를 보강한다.

---

## 목차

1. [현재 문제 전체 분석](#1-현재-문제-전체-분석)
2. [설계 원칙 및 보안 전략](#2-설계-원칙-및-보안-전략)
3. [프로젝트 기존 패턴 정리](#3-프로젝트-기존-패턴-정리)
4. [Phase 1 — 도메인 계층](#phase-1--도메인-계층)
5. [Phase 2 — 인프라 계층 (Redis 3계층)](#phase-2--인프라-계층-redis-3계층)
6. [Phase 3 — 공통 예외 계층](#phase-3--공통-예외-계층)
7. [Phase 4 — 서비스 계층](#phase-4--서비스-계층)
8. [Phase 5 — DTO 계층](#phase-5--dto-계층)
9. [Phase 6 — UseCase 계층](#phase-6--usecase-계층)
10. [Phase 7 — 컨트롤러 계층](#phase-7--컨트롤러-계층)
11. [변경 파일 요약](#변경-파일-요약)
12. [테스트 코드 작성 계획](#테스트-코드-작성-계획)
13. [리스크 및 체크리스트](#리스크-및-체크리스트)

---

## 1. 현재 문제 전체 분석

### 1-1. 호출 스택 — 실패 경로 전체 추적

```
[비로그인 사용자 요청]

── /roadView/practice/start ──────────────────────────────────────────────────
  Controller: @CurrentMember Long memberId
    → CustomAuthenticationPrincipalArgumentResolver
      → nullable = false → throw UnauthorizedException  ← 1차 차단

  @CurrentMemberOrNull 으로 교체한다면 memberId = null 로 통과 →
    StartRoadViewPracticeUseCase.execute(null, sidoKey)
      → memberAdaptor.queryById(null)               ← 문제1: DB 조회 실패
      → memberProfileRedisAdaptor.findProfile(null) ← 문제2: Redis 조회 실패

── /roadView/practice/end ────────────────────────────────────────────────────
  @CurrentMemberOrNull 교체 후 memberId = null 로 통과 →
    EndRoadViewPracticeUseCase.execute(null, request)
      → memberAdaptor.queryById(null)               ← 문제3: DB 조회 실패
      → roadViewGameService.finishGame(null, game, request)
          → game.end(null, ...)  [RoadViewGame:67]
              → super.end(null, ...)  [Game:74]
                  → validateOwnMember(null)  [Game:96]
                      → isNotSameMember(null)  [Game:102]
                          → this.member.getId()  ← 문제4: NPE (game.member=null)
                          → null.getId()         ← 문제5: NPE (파라미터 null)
      → eventPublisher.publishEvent(new RoadViewPracticeEvent(null, game))
          → EndRoadViewPracticeEventListener.handleGameEnd()
              → memberStatisticService.updateSingleGameStatistic(null, ...)
                  ← 문제6: null member로 통계 반영 시도
      → EndGameResponse.RoadViewPractice.from(null, game, coordinate)
          → null.getNickname()              ← 문제7: NPE

── /roadView/{gameId}/reissue-coordinate ─────────────────────────────────────
  @CurrentMemberOrNull 교체 후 memberId = null →
    ReIssueRoadViewCoordinateUseCase.execute(null, gameId)
      → memberAdaptor.queryById(null)       ← 문제8: DB 조회 실패

  [기존 보안 버그 — 로그인 상태에서도]:
    Member member = memberAdaptor.queryById(memberId); // 조회는 함
    RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(gameId);
    // ← member와 game 간 소유권 검증 없음   ← 문제9: 누구나 타인 gameId 재발급 가능
```

### 1-2. 기존 테스트 파손 상태 — 추가 발견

```java
// RoadViewGameServiceTest.java:80 — 현재 컴파일 에러 상태
EndGameResponse.RoadViewPractice response =
    endRoadViewPracticeUseCase.execute(member, request);  // ← Member 객체 전달
// 실제 시그니처: execute(Long memberId, EndGameRequest.RoadView request)
```

이 파일은 이전 리팩토링 시 시그니처가 변경됐으나 테스트가 갱신되지 않아 깨진 상태다.
이번 PR에서 함께 수정한다.

### 1-3. 발견된 문제 목록

| # | 파일 | 위치 | 유형 | 설명 |
|---|------|------|------|------|
| 1 | `StartRoadViewPracticeUseCase` | `:25` | 익명 불가 | `memberAdaptor.queryById(null)` |
| 2 | `StartRoadViewPracticeUseCase` | `:27` | 익명 불가 | `memberProfileRedisAdaptor.findProfile(null)` |
| 3 | `EndRoadViewPracticeUseCase` | `:27` | 익명 불가 | `memberAdaptor.queryById(null)` |
| 4 | `Game` | `:103` | NPE 위험 | `this.member.getId()` — game.member null |
| 5 | `Game` | `:103` | NPE 위험 | member 파라미터 null 시 NPE |
| 6 | `EndRoadViewPracticeEventListener` | `:31` | 데이터 오염 | 익명에 통계 반영 불가 |
| 7 | `EndGameResponse.RoadViewPractice` | `:24` | NPE 위험 | `member.getNickname()` — null 불가 |
| 8 | `ReIssueRoadViewCoordinateUseCase` | `:34` | 익명 불가 | member 조회 필수 구조 |
| 9 | `ReIssueRoadViewCoordinateUseCase` | `:35` | **보안 버그** | 소유권 검증 누락 |
| 10 | `StartRoadViewPracticeUseCase` | `:33` | 코드 스멜 | `getEncryptedRoadViewGameResponse(member, ...)` — member 미사용 파라미터 |
| 11 | `RoadViewGameServiceTest` | `:80` | **컴파일 에러** | `execute(member, request)` 구 시그니처 사용 |

### 1-4. SecurityConfig — 변경 불필요 확인

```java
// SecurityConfig.java:97
.anyRequest().permitAll();  // 모든 URL 이미 허용
```

비로그인 차단은 Spring Security가 아닌 `@CurrentMember` 어노테이션 처리가 담당한다.
`@CurrentMemberOrNull`로 교체하는 것만으로 충분하다.

---

## 2. 설계 원칙 및 보안 전략

### 2-1. 구현 원칙

| 원칙 | 적용 방식 |
|------|---------|
| 기존 코드 경로 불변 | `memberId != null` 분기로 로그인 경로 완전 격리 |
| 도메인 소유권 검증 유지 | `Game.validateOwnMember()`는 로그인 경로에서 반드시 실행 |
| 익명 이벤트 미발행 | `eventPublisher.publishEvent()`는 익명 분기에서 절대 호출하지 않음 |
| 프로젝트 패턴 준수 | Redis 3계층, `@Adaptor`, 키 네이밍 등 기존 패턴 그대로 따름 |
| 단일 책임 | 로그인/익명 로직을 각각 private 메서드로 분리 |

### 2-2. 보안 전략 — Redis UUID 토큰

```
[익명 Start]
  서버: UUID 생성 → Redis 저장 (key="game:practice:anonymous:{gameId}", TTL=2h)
  응답: StartGameResponse.RoadView.practiceToken = UUID

[익명 End / Reissue]
  요청 헤더: X-Practice-Token: {UUID}
  서버:
    1. token == null          → 401 ANONYMOUS_PRACTICE_TOKEN_REQUIRED (4217)
    2. Redis 없음 or 불일치   → 403 ANONYMOUS_PRACTICE_TOKEN_INVALID (4218)
    3. 일치                   → 정상 처리
    4. End 완료 후 Redis 즉시 삭제 (재사용 방지)
```

**JWT 대신 UUID + Redis 선택 이유**
- 프로젝트가 이미 동일 목적(단기 세션 데이터)으로 Redis 활용 중
- TTL 자동 만료 및 강제 삭제 구현이 단순
- 별도 서명 키 관리 불필요

---

## 3. 프로젝트 기존 패턴 정리

### 3-1. Redis 인프라 3계층

```
multi/room/infrastructure/redis/
├── constant/GameRoomRedisKeyConstants.java   ← static 키 상수
│     "game:room:%s:players"
├── dao/GameRoomRedisRepository.java          ← @Repository, RedisTemplate<String, String>
└── adaptor/GameRoomRedisAdaptor.java         ← @Adaptor, 비즈니스 로직
```

신규 구조 (동일 패턴):

```
game/infrastructure/redis/
├── constant/AnonymousPracticeRedisKeyConstants.java
│     "game:practice:anonymous:%s"
├── dao/AnonymousPracticeTokenRedisRepository.java   ← @Repository
└── adaptor/AnonymousPracticeTokenRedisAdaptor.java  ← @Adaptor
```

### 3-2. 사용 어노테이션 근거

| 계층 | 어노테이션 | 근거 |
|------|-----------|------|
| Redis DAO | `@Repository` | `GameRoomRedisRepository`, `MemberProfileRedisRepository` |
| Redis Adaptor | `@Adaptor` | `GameRoomRedisAdaptor`, `RoadViewGameAdaptor` |
| UseCase | `@UseCase` | `StartRoadViewPracticeUseCase` |

### 3-3. BotSuccessAspect — 익명 안전 확인

```java
// BotSuccessAspect.java:53-60
private Member extractMemberFromMethodArgs(ProceedingJoinPoint joinPoint) {
    for (Object arg : joinPoint.getArgs()) {
        if (arg instanceof Member) { return (Member) arg; } // Long 타입은 해당 없음
    }
    return null; // 항상 null
}
// member=null → isBotMember(null)=false → joinPoint.proceed() 정상 호출
```

컨트롤러 파라미터가 `Long memberId` 타입이므로 익명 케이스에서 `@BotSuccess` 동작에 이상 없음.
**변경 불필요**.

---

## Phase 1 — 도메인 계층

**변경 파일**
- `src/main/java/com/kospot/game/domain/entity/Game.java`
- `src/main/java/com/kospot/game/domain/entity/RoadViewGame.java`

### 1-1. `Game.java` — `endAnonymous()` 추가

기존 `end(Member, ...)` 는 소유자 검증을 강제하므로 익명 전용 우회 메서드를 별도 추가한다.
기존 메서드는 한 글자도 변경하지 않는다.

```java
// ▼ 기존 코드 (변경 없음)
public void end(Member member, double submittedLat, double submittedLng, double answerTime) {
    validateOwnMember(member);
    validateGameStatus();
    this.gameStatus = GameStatus.COMPLETED;
    this.submittedLat = submittedLat;
    this.submittedLng = submittedLng;
    this.answerTime = answerTime;
    this.endedAt = LocalDateTime.now();
}

// ▼ 신규 추가 — 익명 전용 (소유자 검증 생략, 상태 검증만 유지)
public void endAnonymous(double submittedLat, double submittedLng, double answerTime) {
    validateGameStatus();   // COMPLETED 중복 종료 방지는 익명에도 동일 적용
    this.gameStatus = GameStatus.COMPLETED;
    this.submittedLat = submittedLat;
    this.submittedLng = submittedLng;
    this.answerTime = answerTime;
    this.endedAt = LocalDateTime.now();
}
```

> `validateOwnMember`, `validateGameStatus`, `isNotSameMember` — 기존 private 메서드 변경 없음.

---

### 1-2. `RoadViewGame.java` — `endAnonymous()` 오버로드 추가

기존 `end(Member, ...)` 유지, 익명 전용만 추가.

```java
// ▼ 기존 코드 (변경 없음)
public void end(Member member, double submittedLat, double submittedLng,
                double answerTime, double answerDistance) {
    super.end(member, submittedLat, submittedLng, answerTime);
    this.answerDistance = answerDistance;
    this.score = calculateGameScore(answerDistance, answerTime);
}

// ▼ 신규 추가 — 익명 전용
public void endAnonymous(double submittedLat, double submittedLng,
                          double answerTime, double answerDistance) {
    super.endAnonymous(submittedLat, submittedLng, answerTime);
    this.answerDistance = answerDistance;
    this.score = calculateGameScore(answerDistance, answerTime);
}
```

> `calculateGameScore()` — `private` 이므로 동일하게 재사용.

---

## Phase 2 — 인프라 계층 (Redis 3계층)

**신규 파일**
- `src/main/java/com/kospot/game/infrastructure/redis/constant/AnonymousPracticeRedisKeyConstants.java`
- `src/main/java/com/kospot/game/infrastructure/redis/dao/AnonymousPracticeTokenRedisRepository.java`
- `src/main/java/com/kospot/game/infrastructure/redis/adaptor/AnonymousPracticeTokenRedisAdaptor.java`

---

### 2-1. `AnonymousPracticeRedisKeyConstants.java`

`GameRoomRedisKeyConstants` 와 동일한 static 유틸 패턴.

```java
package com.kospot.game.infrastructure.redis.constant;

public class AnonymousPracticeRedisKeyConstants {

    private static final String TOKEN_KEY = "game:practice:anonymous:%s";

    public static String getTokenKey(Long gameId) {
        return String.format(TOKEN_KEY, gameId);
    }
}
```

---

### 2-2. `AnonymousPracticeTokenRedisRepository.java`

`GameRoomRedisRepository` 와 동일하게 `RedisTemplate<String, String>` 사용.

```java
package com.kospot.game.infrastructure.redis.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class AnonymousPracticeTokenRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public void save(String key, String token, long expireHours) {
        redisTemplate.opsForValue().set(key, token, expireHours, TimeUnit.HOURS);
    }

    public String find(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
```

---

### 2-3. `AnonymousPracticeTokenRedisAdaptor.java`

`GameRoomRedisAdaptor` 와 동일하게 `@Adaptor` 사용.
UUID 생성·검증·삭제 비즈니스 로직은 이 계층에서만 담당한다.

```java
package com.kospot.game.infrastructure.redis.adaptor;

import com.kospot.common.annotation.adaptor.Adaptor;
import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.game.infrastructure.redis.constant.AnonymousPracticeRedisKeyConstants;
import com.kospot.game.infrastructure.redis.dao.AnonymousPracticeTokenRedisRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Adaptor
@RequiredArgsConstructor
public class AnonymousPracticeTokenRedisAdaptor {

    private static final long TOKEN_EXPIRE_HOURS = 2L;

    private final AnonymousPracticeTokenRedisRepository repository;

    public String generateAndStore(Long gameId) {
        String token = UUID.randomUUID().toString();
        repository.save(AnonymousPracticeRedisKeyConstants.getTokenKey(gameId), token, TOKEN_EXPIRE_HOURS);
        return token;
    }

    public void validate(Long gameId, String token) {
        if (token == null) {
            throw new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED);
        }
        String stored = repository.find(AnonymousPracticeRedisKeyConstants.getTokenKey(gameId));
        if (stored == null || !stored.equals(token)) {
            throw new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID);
        }
    }

    public void delete(Long gameId) {
        repository.delete(AnonymousPracticeRedisKeyConstants.getTokenKey(gameId));
    }
}
```

---

## Phase 3 — 공통 예외 계층

**변경 파일**
- `src/main/java/com/kospot/common/exception/payload/code/ErrorStatus.java`

Game Error 범위: 4211 ~ 4250. 현재 최댓값: 4216 (`GAME_NOT_SAME_MEMBER`).

```java
// Game Error (4211 ~ 4250) 블록 — 기존 4216 다음에 추가
ANONYMOUS_PRACTICE_TOKEN_REQUIRED(UNAUTHORIZED, 4217, "익명 연습 게임 토큰이 필요합니다."),
ANONYMOUS_PRACTICE_TOKEN_INVALID(FORBIDDEN,     4218, "익명 연습 게임 토큰이 유효하지 않습니다."),
```

---

## Phase 4 — 서비스 계층

**변경 파일**
- `src/main/java/com/kospot/game/application/service/RoadViewGameService.java`

기존 메서드는 **한 글자도 변경하지 않는다.** 익명 전용 메서드를 하단에 추가한다.

```java
// ▼ 신규 추가 — 익명 게임 시작 (member = null)
public RoadViewGame startAnonymousPracticeGame(String sidoKey) {
    Sido sido = Sido.fromKey(sidoKey);
    Coordinate coordinate = coordinateAdaptor.getRandomCoordinateBySido(sido);
    RoadViewGame game = RoadViewGame.create(coordinate, null, GameType.PRACTICE, sido);
    repository.save(game);
    return game;
}

// ▼ 신규 추가 — 익명 게임 종료 (소유자 검증 없는 경로)
public RoadViewGame finishGameAnonymous(RoadViewGame game, EndGameRequest.RoadView request) {
    double distance = DistanceCalculator.calculateHaversineDistance(
            request.getSubmittedLat(), request.getSubmittedLng(),
            game.getCoordinate()
    );
    game.endAnonymous(
            request.getSubmittedLat(), request.getSubmittedLng(),
            request.getAnswerTime(), distance
    );
    return game;
}
```

> **DB 사전 확인 필수**: `road_view_game.member_member_id` 컬럼의 DDL NOT NULL 제약 여부 확인.
> JPA `@ManyToOne`에 `optional=false` 가 없으므로 JPA 레벨은 nullable 이지만,
> 실제 DDL이 NOT NULL 이라면 migration 선행 필요.

---

## Phase 5 — DTO 계층

**변경 파일**
- `src/main/java/com/kospot/game/presentation/dto/response/StartGameResponse.java`
- `src/main/java/com/kospot/game/presentation/dto/response/EndGameResponse.java`

### 5-1. `StartGameResponse.RoadView` — `practiceToken` 필드 추가

`StartGameResponse.RoadView` 는 practice / rank 공용이다.
rank 게임 응답에서는 `practiceToken = null` (의도된 동작).

```java
@Getter
@Builder
public static class RoadView {
    private Long gameId;
    private String poiName;
    private String targetLat;
    private String targetLng;
    private String markerImageUrl;

    @Schema(description = "익명 연습 게임 전용 보안 토큰. 로그인 / 랭크 게임은 null.", nullable = true)
    private String practiceToken;  // 신규 추가
}
```

### 5-2. `EndGameResponse.RoadViewPractice` — 익명 팩토리 추가

기존 `from()` 유지, `fromAnonymous()` 추가.

```java
// ▼ 기존 (변경 없음)
public static RoadViewPractice from(Member member, RoadViewGame game, Coordinate coordinate) {
    return RoadViewPractice.builder()
            .nickname(member.getNickname())
            .score(game.getScore())
            .answerDistance(game.getAnswerDistance())
            .fullAddress(coordinate.getAddress().getFullAddress())
            .poiName(coordinate.getPoiName())
            .build();
}

// ▼ 신규 추가 — 익명 전용 (member 불필요)
public static RoadViewPractice fromAnonymous(RoadViewGame game, Coordinate coordinate) {
    return RoadViewPractice.builder()
            .nickname("게스트")
            .score(game.getScore())
            .answerDistance(game.getAnswerDistance())
            .fullAddress(coordinate.getAddress().getFullAddress())
            .poiName(coordinate.getPoiName())
            .build();
}
```

> `EndGameRequest.RoadView` — 변경 없음. `practiceToken` 은 HTTP 헤더로 수신.

---

## Phase 6 — UseCase 계층

**변경 파일**
- `src/main/java/com/kospot/game/application/usecase/practice/usecase/StartRoadViewPracticeUseCase.java`
- `src/main/java/com/kospot/game/application/usecase/practice/usecase/EndRoadViewPracticeUseCase.java`
- `src/main/java/com/kospot/game/application/usecase/rank/ReIssueRoadViewCoordinateUseCase.java`

---

### 6-1. `StartRoadViewPracticeUseCase`

**기존 코드 스멜 동시 수정**: `getEncryptedRoadViewGameResponse(member, game, markerImageUrl)` 에서
`member` 파라미터가 메서드 내부에서 미사용. `buildResponse()` 로 교체하며 제거.

```java
@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewPracticeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final AESService aesService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    private final AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor; // 신규 주입

    public StartGameResponse.RoadView execute(Long memberId, String sidoKey) {
        if (memberId != null) {
            return startAsLoggedIn(memberId, sidoKey);
        }
        return startAsAnonymous(sidoKey);
    }

    // ── 로그인 경로 (기존 로직 이전, 변경 없음) ──────────────────────────
    private StartGameResponse.RoadView startAsLoggedIn(Long memberId, String sidoKey) {
        Member member = memberAdaptor.queryById(memberId);
        RoadViewGame game = roadViewGameService.startPracticeGame(member, sidoKey);
        String markerImageUrl = memberProfileRedisAdaptor.findProfile(memberId).markerImageUrl();
        return buildResponse(game, markerImageUrl, null);
    }

    // ── 익명 경로 ─────────────────────────────────────────────────────────
    private StartGameResponse.RoadView startAsAnonymous(String sidoKey) {
        RoadViewGame game = roadViewGameService.startAnonymousPracticeGame(sidoKey);
        String practiceToken = anonymousPracticeTokenRedisAdaptor.generateAndStore(game.getId());
        return buildResponse(game, null, practiceToken); // markerImageUrl=null → 클라이언트 기본 마커 사용
    }

    // ── 공통 응답 빌더 (기존 스멜 수정: member 파라미터 제거) ─────────────
    private StartGameResponse.RoadView buildResponse(RoadViewGame game,
                                                      String markerImageUrl,
                                                      String practiceToken) {
        return StartGameResponse.RoadView.builder()
                .gameId(game.getId())
                .poiName(game.getCoordinate().getPoiName())
                .targetLat(aesService.toEncryptString(game.getCoordinate().getLat()))
                .targetLng(aesService.toEncryptString(game.getCoordinate().getLng()))
                .markerImageUrl(markerImageUrl)
                .practiceToken(practiceToken)
                .build();
    }
}
```

---

### 6-2. `EndRoadViewPracticeUseCase`

시그니처에 `String practiceToken` 추가.

```java
@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewPracticeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor; // 신규 주입

    public EndGameResponse.RoadViewPractice execute(Long memberId,
                                                     EndGameRequest.RoadView request,
                                                     String practiceToken) {
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(request.getGameId());

        if (memberId != null) {
            return endAsLoggedIn(memberId, game, request);
        }
        return endAsAnonymous(game, request, practiceToken);
    }

    // ── 로그인 경로 (기존 로직 이전, 변경 없음) ──────────────────────────
    private EndGameResponse.RoadViewPractice endAsLoggedIn(Long memberId,
                                                            RoadViewGame game,
                                                            EndGameRequest.RoadView request) {
        Member member = memberAdaptor.queryById(memberId);
        roadViewGameService.finishGame(member, game, request);
        eventPublisher.publishEvent(new RoadViewPracticeEvent(member, game));
        return EndGameResponse.RoadViewPractice.from(member, game, game.getCoordinate());
    }

    // ── 익명 경로 ─────────────────────────────────────────────────────────
    private EndGameResponse.RoadViewPractice endAsAnonymous(RoadViewGame game,
                                                             EndGameRequest.RoadView request,
                                                             String practiceToken) {
        anonymousPracticeTokenRedisAdaptor.validate(game.getId(), practiceToken); // 토큰 검증
        roadViewGameService.finishGameAnonymous(game, request);
        anonymousPracticeTokenRedisAdaptor.delete(game.getId());                  // 즉시 삭제
        return EndGameResponse.RoadViewPractice.fromAnonymous(game, game.getCoordinate());
        // eventPublisher 미호출 → EndRoadViewPracticeEventListener 미실행 → 통계 변경 없음
    }
}
```

---

### 6-3. `ReIssueRoadViewCoordinateUseCase`

시그니처에 `String practiceToken` 추가 + 소유권 검증 버그 수정.

```java
@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class ReIssueRoadViewCoordinateUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final CoordinateService coordinateService;
    private final CoordinateAdaptor coordinateAdaptor;
    private final AESService aesService;
    private final AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor; // 신규 주입

    public StartGameResponse.ReIssue execute(Long memberId, Long gameId, String practiceToken) {
        RoadViewGame game = roadViewGameAdaptor.queryByIdFetchCoordinate(gameId);

        if (memberId != null) {
            Member member = memberAdaptor.queryById(memberId);
            validateOwnership(member, game);     // 기존 버그 수정: 소유권 검증 추가
        } else {
            anonymousPracticeTokenRedisAdaptor.validate(gameId, practiceToken);
        }

        Coordinate coordinate = game.getCoordinate();
        coordinateService.invalidateCoordinate(coordinate);
        Coordinate newCoordinate = getNewCoordinate(game.getPracticeSido());
        roadViewGameService.updateCoordinate(game, newCoordinate);
        return getEncryptedRoadViewGameResponse(game);
    }

    private void validateOwnership(Member member, RoadViewGame game) {
        // game.getMember() == null 이면 익명 게임 → 로그인 사용자는 접근 불가
        if (game.getMember() == null || !Objects.equals(game.getMember().getId(), member.getId())) {
            throw new GameHandler(ErrorStatus.GAME_NOT_SAME_MEMBER);
        }
    }

    // 이하 기존 private 메서드 변경 없음
    private StartGameResponse.ReIssue getEncryptedRoadViewGameResponse(RoadViewGame game) { ... }
    private Coordinate getNewCoordinate(Sido sido) { ... }
}
```

---

## Phase 7 — 컨트롤러 계층

**변경 파일**
- `src/main/java/com/kospot/game/presentation/controller/RoadViewGameController.java`

`@CurrentMember` → `@CurrentMemberOrNull`, `X-Practice-Token` 헤더 파라미터 추가 (3개 엔드포인트).

```java
@Operation(
    summary = "로드뷰 연습 게임 시작",
    description = "비로그인 사용자는 응답의 practiceToken을 저장하여 end/reissue 요청에 사용합니다."
)
@PostMapping("/practice/start")
@BotSuccess
public ApiResponseDto<StartGameResponse.RoadView> startPracticeGame(
        @CurrentMemberOrNull Long memberId,
        @RequestParam("sido") String sidoKey) {
    return ApiResponseDto.onSuccess(startRoadViewPracticeUseCase.execute(memberId, sidoKey));
}

@Operation(
    summary = "로드뷰 연습 게임 종료",
    description = "비로그인 사용자는 X-Practice-Token 헤더에 시작 시 발급된 토큰을 포함해야 합니다."
)
@PostMapping("/practice/end")
public ApiResponseDto<EndGameResponse.RoadViewPractice> endPracticeGame(
        @CurrentMemberOrNull Long memberId,
        @RequestHeader(value = "X-Practice-Token", required = false) String practiceToken,
        @RequestBody EndGameRequest.RoadView request) {
    return ApiResponseDto.onSuccess(endRoadViewPracticeUseCase.execute(memberId, request, practiceToken));
}

@Operation(
    summary = "로드뷰 좌표 재발급",
    description = "비로그인 사용자는 X-Practice-Token 헤더에 시작 시 발급된 토큰을 포함해야 합니다."
)
@PostMapping("/{gameId}/reissue-coordinate")
public ApiResponseDto<StartGameResponse.ReIssue> reissuePracticeCoordinate(
        @CurrentMemberOrNull Long memberId,
        @RequestHeader(value = "X-Practice-Token", required = false) String practiceToken,
        @PathVariable("gameId") Long gameId) {
    return ApiResponseDto.onSuccess(reIssueRoadViewCoordinateUseCase.execute(memberId, gameId, practiceToken));
}
```

---

## 변경 파일 요약

```
[신규 생성 — 3개]
game/infrastructure/redis/
├── constant/AnonymousPracticeRedisKeyConstants.java
├── dao/AnonymousPracticeTokenRedisRepository.java
└── adaptor/AnonymousPracticeTokenRedisAdaptor.java

[도메인 — 메서드 추가만]
├── game/domain/entity/Game.java                             (+endAnonymous)
└── game/domain/entity/RoadViewGame.java                     (+endAnonymous 오버로드)

[서비스 — 메서드 추가만]
└── game/application/service/RoadViewGameService.java        (+startAnonymousPracticeGame, +finishGameAnonymous)

[DTO — 필드/팩토리 추가만]
├── game/presentation/dto/response/StartGameResponse.java    (+practiceToken 필드)
└── game/presentation/dto/response/EndGameResponse.java      (+fromAnonymous)

[UseCase — 분기 추가 + 시그니처 변경]
├── game/application/usecase/practice/usecase/
│   ├── StartRoadViewPracticeUseCase.java                    (기존 스멜 정리 + 익명 분기)
│   └── EndRoadViewPracticeUseCase.java                      (시그니처 변경 + 익명 분기)
└── game/application/usecase/rank/
    └── ReIssueRoadViewCoordinateUseCase.java                (소유권 검증 버그 수정 + 익명 지원)

[컨트롤러 — 어노테이션/파라미터 변경]
└── game/presentation/controller/RoadViewGameController.java  (@CurrentMember→@CurrentMemberOrNull, 헤더 추가)

[공통 — 에러 코드 추가]
└── common/exception/payload/code/ErrorStatus.java           (+4217, +4218)

[기존 테스트 수정 — 컴파일 에러 수정]
└── test/.../game/service/RoadViewGameServiceTest.java        (execute 시그니처 수정)

총계: 신규 3 + 수정 9 + 테스트 수정 1 = 13개 파일
```

---

## 테스트 코드 작성 계획

### 테스트 파일 구조

```
src/test/java/com/kospot/
├── game/
│   ├── domain/
│   │   └── GameAnonymousEndTest.java                        ← 신규 (도메인 단위)
│   ├── infrastructure/redis/
│   │   └── AnonymousPracticeTokenRedisAdaptorTest.java      ← 신규 (Mockito 단위)
│   └── service/
│       └── RoadViewGameServiceTest.java                     ← 기존 수정 (컴파일 에러 수정)
└── application/practice/
    ├── StartRoadViewPracticeUseCaseTest.java                 ← 신규 (Mockito 단위)
    ├── EndRoadViewPracticeUseCaseTest.java                   ← 신규 (Mockito 단위)
    └── ReIssueRoadViewCoordinateUseCaseTest.java             ← 신규 (Mockito 단위)
```

---

### T-1. `GameAnonymousEndTest.java` — 도메인 단위 테스트

패턴: `ScoreCalculatorTest` 와 동일한 순수 도메인 테스트 (Spring 컨텍스트 없음).

```java
package com.kospot.game.domain;

import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.game.domain.vo.GameStatus;
import com.kospot.game.domain.vo.GameType;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.exception.object.domain.GameHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Game 도메인 — 익명 종료 로직")
class GameAnonymousEndTest {

    private RoadViewGame anonymousGame;
    private RoadViewGame loggedInGame;
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .username("user1")
                .nickname("nick1")
                .build();

        anonymousGame = RoadViewGame.builder()
                .gameMode(GameMode.ROADVIEW)
                .gameType(GameType.PRACTICE)
                .gameStatus(GameStatus.ABANDONED)
                .member(null)   // 익명 게임
                .build();

        loggedInGame = RoadViewGame.builder()
                .gameMode(GameMode.ROADVIEW)
                .gameType(GameType.PRACTICE)
                .gameStatus(GameStatus.ABANDONED)
                .member(member) // 로그인 게임
                .build();
    }

    @Test
    @DisplayName("익명 게임 — endAnonymous 호출 시 COMPLETED 상태로 변경된다")
    void anonymousGame_endAnonymous_changesStatusToCompleted() {
        // when
        anonymousGame.endAnonymous(37.5, 127.0, 30.0, 100.0);

        // then
        assertThat(anonymousGame.getGameStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(anonymousGame.getSubmittedLat()).isEqualTo(37.5);
        assertThat(anonymousGame.getSubmittedLng()).isEqualTo(127.0);
        assertThat(anonymousGame.getAnswerTime()).isEqualTo(30.0);
        assertThat(anonymousGame.getAnswerDistance()).isEqualTo(100.0);
        assertThat(anonymousGame.getEndedAt()).isNotNull();
        assertThat(anonymousGame.getScore()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("익명 게임 — COMPLETED 상태에서 endAnonymous 재호출 시 예외 발생")
    void anonymousGame_endAnonymousTwice_throwsException() {
        // given
        anonymousGame.endAnonymous(37.5, 127.0, 30.0, 100.0);

        // when & then
        assertThatThrownBy(() -> anonymousGame.endAnonymous(37.5, 127.0, 30.0, 100.0))
                .isInstanceOf(GameHandler.class);
    }

    @Test
    @DisplayName("로그인 게임 — 소유자 검증 통과 시 end() 정상 동작")
    void loggedInGame_end_withCorrectMember_succeeds() {
        // when
        loggedInGame.end(member, 37.5, 127.0, 30.0, 100.0);

        // then
        assertThat(loggedInGame.getGameStatus()).isEqualTo(GameStatus.COMPLETED);
    }

    @Test
    @DisplayName("로그인 게임 — 다른 멤버로 end() 호출 시 소유권 예외 발생")
    void loggedInGame_end_withWrongMember_throwsOwnershipException() {
        // given
        Member other = Member.builder().id(99L).username("other").nickname("other").build();

        // when & then
        assertThatThrownBy(() -> loggedInGame.end(other, 37.5, 127.0, 30.0, 100.0))
                .isInstanceOf(GameHandler.class);
    }

    @Test
    @DisplayName("로그인 게임 — endAnonymous 는 소유자 검증 없이 종료 가능")
    void loggedInGame_endAnonymous_skipsOwnershipCheck() {
        // when: 다른 멤버 소유 게임이어도 endAnonymous는 통과
        loggedInGame.endAnonymous(37.5, 127.0, 30.0, 100.0);

        // then
        assertThat(loggedInGame.getGameStatus()).isEqualTo(GameStatus.COMPLETED);
    }
}
```

---

### T-2. `AnonymousPracticeTokenRedisAdaptorTest.java` — Mockito 단위 테스트

```java
package com.kospot.game.infrastructure.redis;

import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.infrastructure.redis.dao.AnonymousPracticeTokenRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnonymousPracticeTokenRedisAdaptor 단위 테스트")
class AnonymousPracticeTokenRedisAdaptorTest {

    @Mock
    private AnonymousPracticeTokenRedisRepository repository;

    @InjectMocks
    private AnonymousPracticeTokenRedisAdaptor adaptor;

    private static final Long GAME_ID = 100L;

    @Test
    @DisplayName("generateAndStore — UUID를 생성하여 Redis에 저장하고 반환한다")
    void generateAndStore_savesTokenAndReturnsIt() {
        // when
        String token = adaptor.generateAndStore(GAME_ID);

        // then
        assertThat(token).isNotNull().isNotBlank();
        // UUID 형식 검증
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(repository).save(keyCaptor.capture(), tokenCaptor.capture(), eq(2L));

        assertThat(keyCaptor.getValue()).isEqualTo("game:practice:anonymous:" + GAME_ID);
        assertThat(tokenCaptor.getValue()).isEqualTo(token);
    }

    @Test
    @DisplayName("validate — 저장된 토큰과 일치하면 예외 없이 통과한다")
    void validate_withMatchingToken_passes() {
        // given
        String token = "valid-uuid-token";
        when(repository.find(anyString())).thenReturn(token);

        // when & then (예외 없이 통과)
        adaptor.validate(GAME_ID, token);

        verify(repository).find("game:practice:anonymous:" + GAME_ID);
    }

    @Test
    @DisplayName("validate — 토큰이 null 이면 ANONYMOUS_PRACTICE_TOKEN_REQUIRED 예외 발생")
    void validate_withNullToken_throwsRequiredException() {
        // when & then
        assertThatThrownBy(() -> adaptor.validate(GAME_ID, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED));

        verify(repository, never()).find(anyString());
    }

    @Test
    @DisplayName("validate — Redis에 토큰 없으면 ANONYMOUS_PRACTICE_TOKEN_INVALID 예외 발생")
    void validate_withExpiredToken_throwsInvalidException() {
        // given: TTL 만료 시뮬레이션
        when(repository.find(anyString())).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> adaptor.validate(GAME_ID, "any-token"))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID));
    }

    @Test
    @DisplayName("validate — 토큰 불일치 시 ANONYMOUS_PRACTICE_TOKEN_INVALID 예외 발생")
    void validate_withWrongToken_throwsInvalidException() {
        // given
        when(repository.find(anyString())).thenReturn("stored-token");

        // when & then
        assertThatThrownBy(() -> adaptor.validate(GAME_ID, "different-token"))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID));
    }

    @Test
    @DisplayName("delete — Redis에서 해당 gameId 의 토큰을 삭제한다")
    void delete_removesTokenFromRedis() {
        // when
        adaptor.delete(GAME_ID);

        // then
        verify(repository).delete("game:practice:anonymous:" + GAME_ID);
    }
}
```

---

### T-3. `StartRoadViewPracticeUseCaseTest.java` — Mockito 단위 테스트

패턴: `LeaveGameRoomUseCaseTest` 와 동일한 `@ExtendWith(MockitoExtension.class)` 구조.

```java
package com.kospot.application.practice;

import com.kospot.game.application.service.AESService;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.application.usecase.practice.usecase.StartRoadViewPracticeUseCase;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.presentation.dto.response.StartGameResponse;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.coordinate.domain.entity.Coordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartRoadViewPracticeUseCase 단위 테스트")
class StartRoadViewPracticeUseCaseTest {

    @Mock private MemberAdaptor memberAdaptor;
    @Mock private RoadViewGameService roadViewGameService;
    @Mock private AESService aesService;
    @Mock private MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    @Mock private AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    @InjectMocks
    private StartRoadViewPracticeUseCase useCase;

    private Member member;
    private RoadViewGame game;
    private Coordinate coordinate;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).username("user1").nickname("nick1").build();

        coordinate = mock(Coordinate.class);
        when(coordinate.getPoiName()).thenReturn("테스트 POI");
        when(coordinate.getLat()).thenReturn(37.5);
        when(coordinate.getLng()).thenReturn(127.0);

        game = mock(RoadViewGame.class);
        when(game.getId()).thenReturn(10L);
        when(game.getCoordinate()).thenReturn(coordinate);

        when(aesService.toEncryptString(anyDouble())).thenReturn("encrypted");
    }

    @Test
    @DisplayName("로그인 사용자 — memberAdaptor, memberProfileRedisAdaptor 를 호출하고 practiceToken=null 반환")
    void execute_withLoggedInMember_usesExistingFlow() {
        // given
        when(memberAdaptor.queryById(1L)).thenReturn(member);
        when(roadViewGameService.startPracticeGame(member, "SEOUL")).thenReturn(game);
        MemberProfileRedisAdaptor.MemberProfileView profile =
                new MemberProfileRedisAdaptor.MemberProfileView(1L, "nick1", "http://marker.url");
        when(memberProfileRedisAdaptor.findProfile(1L)).thenReturn(profile);

        // when
        StartGameResponse.RoadView response = useCase.execute(1L, "SEOUL");

        // then
        assertThat(response.getPracticeToken()).isNull();
        assertThat(response.getMarkerImageUrl()).isEqualTo("http://marker.url");
        assertThat(response.getGameId()).isEqualTo(10L);

        verify(memberAdaptor).queryById(1L);
        verify(memberProfileRedisAdaptor).findProfile(1L);
        verify(anonymousPracticeTokenRedisAdaptor, never()).generateAndStore(anyLong());
    }

    @Test
    @DisplayName("익명 사용자 — memberAdaptor, memberProfileRedisAdaptor 미호출, practiceToken 반환")
    void execute_withAnonymous_skipsLoginFlowAndReturnsPracticeToken() {
        // given
        when(roadViewGameService.startAnonymousPracticeGame("SEOUL")).thenReturn(game);
        when(anonymousPracticeTokenRedisAdaptor.generateAndStore(10L)).thenReturn("uuid-token");

        // when
        StartGameResponse.RoadView response = useCase.execute(null, "SEOUL");

        // then
        assertThat(response.getPracticeToken()).isEqualTo("uuid-token");
        assertThat(response.getMarkerImageUrl()).isNull();
        assertThat(response.getGameId()).isEqualTo(10L);

        verify(memberAdaptor, never()).queryById(anyLong());
        verify(memberProfileRedisAdaptor, never()).findProfile(anyLong());
        verify(anonymousPracticeTokenRedisAdaptor).generateAndStore(10L);
    }
}
```

---

### T-4. `EndRoadViewPracticeUseCaseTest.java` — Mockito 단위 테스트

이벤트 미발행 검증이 핵심.

```java
package com.kospot.application.practice;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.application.usecase.practice.usecase.EndRoadViewPracticeUseCase;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.event.RoadViewPracticeEvent;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.presentation.dto.request.EndGameRequest;
import com.kospot.game.presentation.dto.response.EndGameResponse;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EndRoadViewPracticeUseCase 단위 테스트")
class EndRoadViewPracticeUseCaseTest {

    @Mock private MemberAdaptor memberAdaptor;
    @Mock private RoadViewGameAdaptor roadViewGameAdaptor;
    @Mock private RoadViewGameService roadViewGameService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    @InjectMocks
    private EndRoadViewPracticeUseCase useCase;

    private Member member;
    private RoadViewGame game;
    private Coordinate coordinate;
    private EndGameRequest.RoadView request;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).username("user1").nickname("nick1").build();

        coordinate = mock(Coordinate.class);
        when(coordinate.getPoiName()).thenReturn("테스트 POI");
        when(coordinate.getAddress()).thenReturn(mock(com.kospot.coordinate.domain.entity.Address.class));

        game = mock(RoadViewGame.class);
        when(game.getId()).thenReturn(10L);
        when(game.getCoordinate()).thenReturn(coordinate);
        when(game.getScore()).thenReturn(850.0);
        when(game.getAnswerDistance()).thenReturn(25.0);

        request = EndGameRequest.RoadView.builder()
                .gameId(10L)
                .submittedLat(37.5)
                .submittedLng(127.0)
                .answerTime(30.0)
                .build();

        when(roadViewGameAdaptor.queryByIdFetchCoordinate(10L)).thenReturn(game);
    }

    @Test
    @DisplayName("로그인 사용자 — 기존 흐름 유지: memberAdaptor 호출, 이벤트 발행")
    void execute_withLoggedInMember_publishesEvent() {
        // given
        when(memberAdaptor.queryById(1L)).thenReturn(member);

        // when
        useCase.execute(1L, request, null);

        // then
        verify(memberAdaptor).queryById(1L);
        verify(roadViewGameService).finishGame(eq(member), eq(game), eq(request));
        verify(eventPublisher).publishEvent(any(RoadViewPracticeEvent.class));
        verify(anonymousPracticeTokenRedisAdaptor, never()).validate(anyLong(), anyString());
    }

    @Test
    @DisplayName("로그인 사용자 — 응답에 실제 닉네임 반환")
    void execute_withLoggedInMember_returnsRealNickname() {
        // given
        when(memberAdaptor.queryById(1L)).thenReturn(member);

        // when
        EndGameResponse.RoadViewPractice response = useCase.execute(1L, request, null);

        // then
        assertThat(response.getNickname()).isEqualTo("nick1");
    }

    @Test
    @DisplayName("익명 사용자 — 유효한 토큰으로 종료 성공, 이벤트 미발행")
    void execute_withAnonymous_validToken_doesNotPublishEvent() {
        // given: validate 는 예외 없이 통과 (정상 토큰)
        doNothing().when(anonymousPracticeTokenRedisAdaptor).validate(10L, "valid-token");

        // when
        useCase.execute(null, request, "valid-token");

        // then
        verify(anonymousPracticeTokenRedisAdaptor).validate(10L, "valid-token");
        verify(roadViewGameService).finishGameAnonymous(eq(game), eq(request));
        verify(anonymousPracticeTokenRedisAdaptor).delete(10L);
        verify(eventPublisher, never()).publishEvent(any());          // ← 핵심 검증
        verify(memberAdaptor, never()).queryById(anyLong());
    }

    @Test
    @DisplayName("익명 사용자 — 응답 닉네임이 '게스트'")
    void execute_withAnonymous_returnsGuestNickname() {
        // given
        doNothing().when(anonymousPracticeTokenRedisAdaptor).validate(anyLong(), anyString());

        // when
        EndGameResponse.RoadViewPractice response = useCase.execute(null, request, "valid-token");

        // then
        assertThat(response.getNickname()).isEqualTo("게스트");
    }

    @Test
    @DisplayName("익명 사용자 — 토큰 null 시 ANONYMOUS_PRACTICE_TOKEN_REQUIRED 예외")
    void execute_withAnonymous_nullToken_throwsRequiredException() {
        // given
        doThrow(new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED))
                .when(anonymousPracticeTokenRedisAdaptor).validate(10L, null);

        // when & then
        assertThatThrownBy(() -> useCase.execute(null, request, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("익명 사용자 — 잘못된 토큰으로 ANONYMOUS_PRACTICE_TOKEN_INVALID 예외")
    void execute_withAnonymous_invalidToken_throwsInvalidException() {
        // given
        doThrow(new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID))
                .when(anonymousPracticeTokenRedisAdaptor).validate(10L, "wrong-token");

        // when & then
        assertThatThrownBy(() -> useCase.execute(null, request, "wrong-token"))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID));

        verify(roadViewGameService, never()).finishGameAnonymous(any(), any());
        verify(anonymousPracticeTokenRedisAdaptor, never()).delete(anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("익명 사용자 — 토큰 검증 실패 시 delete 미호출 (원자성 보장)")
    void execute_withAnonymous_tokenValidationFails_doesNotDeleteToken() {
        // given
        doThrow(new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID))
                .when(anonymousPracticeTokenRedisAdaptor).validate(anyLong(), anyString());

        // when & then
        assertThatThrownBy(() -> useCase.execute(null, request, "wrong-token"));

        verify(anonymousPracticeTokenRedisAdaptor, never()).delete(anyLong());
    }
}
```

---

### T-5. `ReIssueRoadViewCoordinateUseCaseTest.java` — Mockito 단위 테스트

기존 보안 버그 수정 검증이 핵심.

```java
package com.kospot.application.practice;

import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.coordinate.application.service.CoordinateService;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.application.service.AESService;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.game.application.usecase.rank.ReIssueRoadViewCoordinateUseCase;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReIssueRoadViewCoordinateUseCase 단위 테스트")
class ReIssueRoadViewCoordinateUseCaseTest {

    @Mock private MemberAdaptor memberAdaptor;
    @Mock private RoadViewGameAdaptor roadViewGameAdaptor;
    @Mock private RoadViewGameService roadViewGameService;
    @Mock private CoordinateService coordinateService;
    @Mock private CoordinateAdaptor coordinateAdaptor;
    @Mock private AESService aesService;
    @Mock private AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    @InjectMocks
    private ReIssueRoadViewCoordinateUseCase useCase;

    private Member owner;
    private Member other;
    private RoadViewGame ownerGame;
    private RoadViewGame anonymousGame;
    private Coordinate oldCoordinate;
    private Coordinate newCoordinate;

    @BeforeEach
    void setUp() {
        owner = Member.builder().id(1L).username("owner").nickname("ownerNick").build();
        other = Member.builder().id(2L).username("other").nickname("otherNick").build();

        oldCoordinate = mock(Coordinate.class);
        when(oldCoordinate.getPoiName()).thenReturn("OLD POI");
        when(oldCoordinate.getLat()).thenReturn(37.5);
        when(oldCoordinate.getLng()).thenReturn(127.0);

        newCoordinate = mock(Coordinate.class);
        when(newCoordinate.getPoiName()).thenReturn("NEW POI");
        when(newCoordinate.getLat()).thenReturn(37.6);
        when(newCoordinate.getLng()).thenReturn(127.1);

        ownerGame = mock(RoadViewGame.class);
        when(ownerGame.getMember()).thenReturn(owner);
        when(ownerGame.getCoordinate()).thenReturn(oldCoordinate);
        when(ownerGame.getPracticeSido()).thenReturn(null);

        anonymousGame = mock(RoadViewGame.class);
        when(anonymousGame.getMember()).thenReturn(null);  // 익명 게임
        when(anonymousGame.getCoordinate()).thenReturn(oldCoordinate);
        when(anonymousGame.getPracticeSido()).thenReturn(null);

        when(coordinateAdaptor.getRandomCoordinate()).thenReturn(newCoordinate);
        when(aesService.toEncryptString(anyDouble())).thenReturn("encrypted");
    }

    @Test
    @DisplayName("로그인 사용자 — 본인 게임 재발급 성공")
    void execute_withOwner_reissuesSuccessfully() {
        // given
        when(roadViewGameAdaptor.queryByIdFetchCoordinate(10L)).thenReturn(ownerGame);
        when(memberAdaptor.queryById(1L)).thenReturn(owner);

        // when
        useCase.execute(1L, 10L, null);

        // then
        verify(coordinateService).invalidateCoordinate(oldCoordinate);
        verify(roadViewGameService).updateCoordinate(ownerGame, newCoordinate);
        verify(anonymousPracticeTokenRedisAdaptor, never()).validate(anyLong(), anyString());
    }

    @Test
    @DisplayName("로그인 사용자 — 타인 게임 재발급 시 GAME_NOT_SAME_MEMBER 예외 (기존 버그 수정 검증)")
    void execute_withOtherMember_throwsOwnershipException() {
        // given
        when(roadViewGameAdaptor.queryByIdFetchCoordinate(10L)).thenReturn(ownerGame);
        when(memberAdaptor.queryById(2L)).thenReturn(other);

        // when & then
        assertThatThrownBy(() -> useCase.execute(2L, 10L, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.GAME_NOT_SAME_MEMBER));

        verify(coordinateService, never()).invalidateCoordinate(any());
    }

    @Test
    @DisplayName("로그인 사용자 — 익명 게임(member=null) 접근 시 GAME_NOT_SAME_MEMBER 예외")
    void execute_withLoggedInMember_onAnonymousGame_throwsOwnershipException() {
        // given
        when(roadViewGameAdaptor.queryByIdFetchCoordinate(20L)).thenReturn(anonymousGame);
        when(memberAdaptor.queryById(1L)).thenReturn(owner);

        // when & then
        assertThatThrownBy(() -> useCase.execute(1L, 20L, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.GAME_NOT_SAME_MEMBER));
    }

    @Test
    @DisplayName("익명 사용자 — 유효한 토큰으로 재발급 성공")
    void execute_withAnonymous_validToken_reissuesSuccessfully() {
        // given
        when(roadViewGameAdaptor.queryByIdFetchCoordinate(20L)).thenReturn(anonymousGame);
        doNothing().when(anonymousPracticeTokenRedisAdaptor).validate(20L, "valid-token");

        // when
        useCase.execute(null, 20L, "valid-token");

        // then
        verify(anonymousPracticeTokenRedisAdaptor).validate(20L, "valid-token");
        verify(memberAdaptor, never()).queryById(anyLong());
        verify(coordinateService).invalidateCoordinate(oldCoordinate);
        verify(roadViewGameService).updateCoordinate(anonymousGame, newCoordinate);
    }

    @Test
    @DisplayName("익명 사용자 — 잘못된 토큰으로 재발급 시도 시 예외 발생")
    void execute_withAnonymous_invalidToken_throwsException() {
        // given
        when(roadViewGameAdaptor.queryByIdFetchCoordinate(20L)).thenReturn(anonymousGame);
        doThrow(new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID))
                .when(anonymousPracticeTokenRedisAdaptor).validate(20L, "wrong-token");

        // when & then
        assertThatThrownBy(() -> useCase.execute(null, 20L, "wrong-token"))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID));

        verify(coordinateService, never()).invalidateCoordinate(any());
    }
}
```

---

### T-6. `RoadViewGameServiceTest.java` — 기존 파일 수정 (컴파일 에러 수정)

**수정 내용**: `execute(member, request)` → `execute(member.getId(), request, null)` 시그니처 업데이트.

```java
// 기존 (컴파일 에러)
EndGameResponse.RoadViewPractice response = endRoadViewPracticeUseCase.execute(member, request);

// 수정 후
EndGameResponse.RoadViewPractice response =
    endRoadViewPracticeUseCase.execute(member.getId(), request, null);
```

> 추가적으로 테스트 본문의 `PointCalculator.getPracticePoint()` 호출 등 현재 존재 여부 확인 필요.
> 스텝별로 컴파일 통과 확인 후 진행한다.

---

### 테스트 커버리지 요약

| 파일 | 유형 | 케이스 수 | 핵심 검증 |
|------|------|---------|---------|
| `GameAnonymousEndTest` | 도메인 순수 | 4 | `endAnonymous` COMPLETED 전환, 중복 종료 방지, 로그인 end 회귀 |
| `AnonymousPracticeTokenRedisAdaptorTest` | Mockito 단위 | 5 | UUID 저장/검증/삭제, null/만료/불일치 예외 |
| `StartRoadViewPracticeUseCaseTest` | Mockito 단위 | 2 | 로그인 기존 흐름 유지, 익명 토큰 생성 |
| `EndRoadViewPracticeUseCaseTest` | Mockito 단위 | 6 | **이벤트 미발행**, 게스트 닉네임, 토큰 검증 실패 시 delete 미호출 |
| `ReIssueRoadViewCoordinateUseCaseTest` | Mockito 단위 | 5 | **소유권 버그 수정**, 익명 게임 접근 차단, 토큰 기반 재발급 |
| `RoadViewGameServiceTest` | 기존 수정 | - | 컴파일 에러 시그니처 수정 |

---

## 리스크 및 체크리스트

### 구현 전 필수 확인

| 항목 | 확인 방법 | 필요 조치 |
|------|---------|---------|
| `road_view_game.member_member_id` NULL 허용 | DDL 또는 Flyway migration 파일 확인 | NOT NULL 이면 migration으로 nullable 변경 선행 |
| `RedisTemplate<String, String>` Bean 등록 | Redis 설정 파일 확인 | 미등록 시 Bean 추가 |

### 구현 리스크

| # | 리스크 | 원인 | 대응 |
|---|--------|------|------|
| 1 | DB 저장 실패 | `member_member_id NOT NULL` 제약 | migration 선행 필수 |
| 2 | 익명 게임 DB 누적 | member 없는 레코드 무제한 생성 | 히스토리 쿼리에서 `member IS NOT NULL` 조건 유무 확인. 배치 정리 정책 별도 검토 |
| 3 | Redis 장애 시 익명 end 전면 차단 | 토큰 검증 불가 | Practice 모드는 통계 미반영이므로 fail-open 정책 검토 가능 (운영 판단 필요) |
| 4 | `reissue-coordinate` 기존 클라이언트 | 소유권 검증 추가로 타인 gameId 사용 차단 | 정상 사용에서는 본인 gameId만 사용하므로 영향 없을 것으로 예상. 통합 회귀 테스트로 확인 |
