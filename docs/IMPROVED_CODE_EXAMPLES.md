# 🔧 개선된 코드 예시

## 1. 개선된 MultiGame 엔티티

```java
package com.kospot.domain.multi.game.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.infrastructure.exception.object.domain.GameHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class MultiGame extends BaseTimeEntity {

    private String title;

    @Enumerated(EnumType.STRING)
    private PlayerMatchType matchType;
    
    @Enumerated(EnumType.STRING)
    private GameMode gameMode;

    @Min(1)
    @Max(15)
    private Integer totalRounds;

    private Integer currentRound;

    private Boolean isFinished;
    
    // ✨ 개선: TimeLimit를 Game 레벨에서 관리
    @Min(10)
    @Max(180)
    private Integer timeLimit; // 초 단위
    
    // Business methods
    public void startGame() {
        this.currentRound = 1;
        this.isFinished = false;
    }
    
    public void moveToNextRound() {
        if (isLastRound()) {
            finishGame();
            return;
        }
        this.currentRound++;
    }
    
    public void finishGame() {
        this.isFinished = true;
    }
    
    public boolean isLastRound() {
        return this.currentRound.equals(this.totalRounds);
    }

    public boolean isCooperativeMode() {
        return PlayerMatchType.TEAM.equals(this.matchType);
    }
    
    public boolean isPhotoMode() {
        return GameMode.PHOTO.equals(this.gameMode);
    }
    
    /**
     * ✨ 개선: TimeLimit를 Duration으로 반환
     * null이면 GameMode의 기본값 사용
     */
    public Duration getTimeLimit() {
        if (timeLimit != null) {
            return Duration.ofSeconds(timeLimit);
        }
        return gameMode.getDuration(); // 기본값
    }
    
    /**
     * ✨ 개선: TimeLimit 검증 로직
     */
    protected void validateTimeLimit(Integer timeLimit) {
        if (timeLimit != null && (timeLimit < 10 || timeLimit > 180)) {
            throw new GameHandler(ErrorStatus.INVALID_TIME_LIMIT);
        }
    }
    
    /**
     * ✨ 개선: TimeLimit이 설정되었는지 확인
     */
    public boolean hasCustomTimeLimit() {
        return timeLimit != null;
    }
}
```

## 2. 개선된 BaseGameRound 엔티티

```java
package com.kospot.domain.multi.round.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiGame;
import com.kospot.infrastructure.exception.object.domain.GameRoundHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseGameRound extends BaseTimeEntity {

    @Builder.Default
    private String roundId = UUID.randomUUID().toString();
    
    private Integer roundNumber;
    
    private Boolean isFinished = false;
    
    // ✨ 개선: timeLimit 필드 제거! Game에서 가져옴
    
    private Instant serverStartTime;

    @Builder.Default
    private List<Long> playerIds = new ArrayList<>();

    /**
     * ✨ 개선: 상위 Game 엔티티를 반환하는 추상 메서드
     * 각 구체 클래스(RoadViewGameRound, PhotoGameRound)에서 구현
     */
    public abstract MultiGame getGame();
    
    public abstract GameMode getGameMode();

    /**
     * ✨ 개선: Game에서 timeLimit 가져오기
     */
    public Duration getDuration() {
        return getGame().getTimeLimit();
    }

    public void startRound() {
        this.serverStartTime = Instant.now();
    }

    /**
     * 남은 시간 계산 (밀리초)
     */
    public long getRemainingTimeMs() {
        if (this.serverStartTime == null) {
            return getDuration().toMillis();
        }
        Duration elapsed = Duration.between(this.serverStartTime, Instant.now());
        long remaining = getDuration().toMillis() - elapsed.toMillis();
        return Math.max(remaining, 0);
    }

    /**
     * 타이머 종료 여부
     */
    public boolean isTimeExpired() {
        return getRemainingTimeMs() <= 0;
    }

    public void finishRound() {
        validateRoundNotFinished();
        this.isFinished = true;
    }
    
    private void validateRoundNotFinished() {
        if (this.isFinished) {
            throw new GameRoundHandler(ErrorStatus.GAME_ROUND_ALREADY_FINISHED);
        }
    }
}
```

## 3. 개선된 RoadViewGameRound 엔티티

```java
package com.kospot.domain.multi.round.entity;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.entity.MultiGame;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewTeamSubmission;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoadViewGameRound extends BaseGameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multi_road_view_game_id")
    private MultiRoadViewGame multiRoadViewGame;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinate_id")
    private CoordinateNationwide targetCoordinate;

    @Builder.Default
    @OneToMany(mappedBy = "roadViewGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewPlayerSubmission> roadViewPlayerSubmissions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "roadViewGameRound", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoadViewTeamSubmission> roadViewTeamSubmissions = new ArrayList<>();

    /**
     * ✨ 개선: 상위 Game 반환 (timeLimit 접근용)
     */
    @Override
    public MultiGame getGame() {
        return this.multiRoadViewGame;
    }

    @Override
    public GameMode getGameMode() {
        return GameMode.ROADVIEW;
    }
    
    // Business methods
    public void setMultiRoadViewGame(MultiRoadViewGame multiRoadViewGame) {
        this.multiRoadViewGame = multiRoadViewGame;
    }
    
    public void addPlayerSubmission(RoadViewPlayerSubmission submission) {
        this.roadViewPlayerSubmissions.add(submission);
        submission.setRoadViewGameRound(this);
    }
    
    public void addTeamSubmission(RoadViewTeamSubmission submission) {
        this.roadViewTeamSubmissions.add(submission);
        submission.setRoadViewGameRound(this);
    }
    
    /**
     * ✨ 개선: 생성 메서드에서 timeLimit 파라미터 제거
     * Game에서 자동으로 참조하도록 변경
     */
    public static RoadViewGameRound createRound(
            Integer roundNumber,
            CoordinateNationwide targetCoordinate,
            List<Long> playerIds) {
        return RoadViewGameRound.builder()
                .roundNumber(roundNumber)
                .targetCoordinate(targetCoordinate)
                .playerIds(playerIds)
                .isFinished(false)
                .build();
    }
}
```

## 4. 개선된 MultiRoadViewGame 엔티티

```java
package com.kospot.domain.multi.game.entity;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.room.entity.GameRoom;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MultiRoadViewGame extends MultiGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_room_id")
    private GameRoom gameRoom;
    
    /**
     * ✨ 개선: timeLimit 추가
     */
    public static MultiRoadViewGame createGame(
            GameRoom gameRoom,
            PlayerMatchType matchType,
            Integer roundCount,
            Integer timeLimit) {
        return MultiRoadViewGame.builder()
                .matchType(matchType)
                .gameMode(GameMode.ROADVIEW)
                .totalRounds(roundCount)
                .currentRound(0)
                .isFinished(false)
                .timeLimit(timeLimit) // ✨ Game 레벨에 timeLimit 저장
                .gameRoom(gameRoom)
                .build();
    }
}
```

## 5. 개선된 RoadViewGameRoundService

```java
package com.kospot.domain.multi.round.service;

import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.repository.RoadViewGameRoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewGameRoundService {

    private final CoordinateService coordinateService;
    private final RoadViewGameRoundRepository roundRepository;

    /**
     * ✨ 개선: timeLimit 파라미터 제거
     * Game에서 자동으로 참조하도록 변경
     */
    public RoadViewGameRound createGameRound(
            MultiRoadViewGame game,
            int roundNumber,
            List<Long> playerIds) {
        
        CoordinateNationwide coordinate = 
            (CoordinateNationwide) coordinateService.getRandomNationwideCoordinate();
        
        RoadViewGameRound gameRound = RoadViewGameRound.createRound(
            roundNumber,
            coordinate,
            playerIds
        );
        
        gameRound.setMultiRoadViewGame(game);
        gameRound.startRound();
        
        log.info("라운드 생성: gameId={}, round={}, timeLimit={}초",
                game.getId(),
                roundNumber,
                game.getTimeLimit().getSeconds());
        
        return roundRepository.save(gameRound);
    }

    /**
     * ✨ 개선: 첫 라운드 생성 전용 메서드 (가독성 향상)
     */
    public RoadViewGameRound createFirstRound(
            MultiRoadViewGame game,
            List<Long> playerIds) {
        return createGameRound(game, 1, playerIds);
    }

    public void endGameRound(RoadViewGameRound round) {
        round.finishRound();
        log.info("라운드 종료: roundId={}", round.getRoundId());
    }
}
```

## 6. 개선된 MultiRoadViewGameService

```java
package com.kospot.domain.multi.game.service;

import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.game.repository.MultiRoadViewGameRepository;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MultiRoadViewGameService {

    private final MultiRoadViewGameRepository multiRoadViewGameRepository;

    /**
     * ✨ 개선: timeLimit을 Game 레벨에 저장
     */
    public MultiRoadViewGame createGame(GameRoom gameRoom, MultiGameRequest.Start request) {
        PlayerMatchType matchType = PlayerMatchType.fromKey(request.getPlayerMatchTypeKey());
        int totalRounds = request.getTotalRounds();
        Integer timeLimit = request.getTimeLimit();
        
        MultiRoadViewGame game = MultiRoadViewGame.createGame(
            gameRoom,
            matchType,
            totalRounds,
            timeLimit // ✨ Game 레벨에 timeLimit 저장
        );
        
        MultiRoadViewGame savedGame = multiRoadViewGameRepository.save(game);
        
        log.info("게임 생성: gameId={}, roomId={}, rounds={}, timeLimit={}초",
                savedGame.getId(),
                gameRoom.getId(),
                totalRounds,
                timeLimit);
        
        return savedGame;
    }
}
```

## 7. 개선된 StartRoadViewSoloRoundUseCase

```java
package com.kospot.application.multi.round;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.service.MultiRoadViewGameService;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewSoloRoundUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final MultiRoadViewGameService multiRoadViewGameService;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerService gamePlayerService;
    private final GameTimerService gameTimerService;

    /**
     * 멀티 로드뷰 개인전 게임 시작
     * 
     * ✨ 개선 사항:
     * 1. GameRoom -> Game -> Round 계층 구조 명확화
     * 2. timeLimit은 Game 레벨에 저장
     * 3. 로깅 강화
     */
    public MultiRoadViewGameResponse.StartPlayerGame execute(
            Member host,
            MultiGameRequest.Start request) {
        
        // 1. GameRoom 조회 및 상태 변경
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(request.getGameRoomId());
        gameRoom.start(host);
        
        log.info("게임 방 시작: roomId={}, host={}", gameRoom.getId(), host.getId());
        
        return startRoadViewGame(gameRoom, request);
    }

    private MultiRoadViewGameResponse.StartPlayerGame startRoadViewGame(
            GameRoom gameRoom,
            MultiGameRequest.Start request) {
        
        // 2. 게임 생성 (timeLimit은 Game에 저장됨)
        MultiRoadViewGame game = multiRoadViewGameService.createGame(gameRoom, request);
        game.startGame();

        // 3. 플레이어 생성
        List<GamePlayer> gamePlayers = gamePlayerService.createRoadViewGamePlayers(
            gameRoom,
            game
        );
        List<Long> playerIds = gamePlayers.stream()
                .map(GamePlayer::getId)
                .toList();

        // 4. ✨ 개선: 첫 라운드 생성 (timeLimit 파라미터 제거)
        RoadViewGameRound round = roadViewGameRoundService.createFirstRound(
            game,
            playerIds
        );

        // 5. 타이머 시작
        TimerCommand command = createTimerCommand(gameRoom.getId(), game, round);
        gameTimerService.startRoundTimer(command);
        
        log.info("게임 시작 완료: gameId={}, roundId={}, players={}",
                game.getId(), round.getRoundId(), playerIds.size());
        
        return MultiRoadViewGameResponse.StartPlayerGame.from(game, round, gamePlayers);
    }

    private TimerCommand createTimerCommand(
            Long gameRoomId,
            MultiRoadViewGame game,
            BaseGameRound round) {
        return TimerCommand.builder()
                .round(round)
                .gameRoomId(gameRoomId.toString())
                .gameId(game.getId().toString())
                .gameMode(GameMode.ROADVIEW)
                .matchType(PlayerMatchType.SOLO)
                .build();
    }
}
```

## 8. 개선된 NextRoadViewRoundUseCase

```java
package com.kospot.application.multi.round;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.multi.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multi.game.entity.MultiRoadViewGame;
import com.kospot.domain.multi.game.vo.PlayerMatchType;
import com.kospot.domain.multi.round.entity.BaseGameRound;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.timer.service.GameTimerService;
import com.kospot.infrastructure.websocket.domain.multi.timer.vo.TimerCommand;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class NextRoadViewRoundUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GameTimerService gameTimerService;

    /**
     * ✨ 개선: timeLimit 파라미터 제거
     * Game에서 자동으로 참조
     */
    public MultiRoadViewGameResponse.NextRound execute(
            Long gameRoomId,
            Long multiGameId) {
        
        MultiRoadViewGame game = multiRoadViewGameAdaptor.queryById(multiGameId);
        game.moveToNextRound();
        
        int currentRound = game.getCurrentRound();
        
        //todo playerIds를 redis에서 가져오기
        // List<Long> playerIds = gamePlayerRedisRepository.findPlayerIdsByGameId(multiGameId);
        List<Long> playerIds = null;
        
        // ✨ 개선: timeLimit 파라미터 제거
        RoadViewGameRound round = roadViewGameRoundService.createGameRound(
            game,
            currentRound,
            playerIds
        );

        TimerCommand command = createTimerCommand(gameRoomId, game, round);
        gameTimerService.startRoundTimer(command);
        
        log.info("다음 라운드 시작: gameId={}, round={}/{}, timeLimit={}초",
                game.getId(),
                currentRound,
                game.getTotalRounds(),
                game.getTimeLimit().getSeconds());
        
        return MultiRoadViewGameResponse.NextRound.from(game, round);
    }

    private TimerCommand createTimerCommand(
            Long gameRoomId,
            MultiRoadViewGame game,
            BaseGameRound round) {
        return TimerCommand.builder()
                .round(round)
                .gameRoomId(gameRoomId.toString())
                .gameId(game.getId().toString())
                .gameMode(GameMode.ROADVIEW)
                .matchType(game.getMatchType())
                .build();
    }
}
```

## 9. 개선된 MultiRoadViewGameController

```java
package com.kospot.presentation.multi.game.controller;

import com.kospot.application.multi.round.EndRoadViewSoloRoundUseCase;
import com.kospot.application.multi.round.NextRoadViewRoundUseCase;
import com.kospot.application.multi.round.StartRoadViewSoloRoundUseCase;
import com.kospot.application.multi.submission.SubmitRoadViewPlayerAnswerUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.multi.game.dto.request.MultiGameRequest;
import com.kospot.presentation.multi.game.dto.response.MultiRoadViewGameResponse;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import com.kospot.presentation.multi.submission.dto.request.SubmissionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ✨ 개선된 Controller
 * - RESTful 계층 구조 명확화: GameRoom > Game > Round
 * - 경로에 gameRoomId 포함
 * - Validation 강화
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Multi RoadView Game API", description = "멀티 로드뷰 게임 API")
@RequestMapping("/api/v1/game-rooms/{gameRoomId}/roadview")
public class MultiRoadViewGameController {

    private final StartRoadViewSoloRoundUseCase startRoadViewSoloRoundUseCase;
    private final SubmitRoadViewPlayerAnswerUseCase submitRoadViewPlayerAnswerUseCase;
    private final NextRoadViewRoundUseCase nextRoadViewRoundUseCase;
    private final EndRoadViewSoloRoundUseCase endRoadViewSoloRoundUseCase;

    @Operation(
        summary = "멀티 로드뷰 개인전 게임 시작",
        description = "게임 방에서 새로운 로드뷰 개인전 게임을 시작합니다."
    )
    @PostMapping("/games/start")
    public ApiResponseDto<MultiRoadViewGameResponse.StartPlayerGame> startPlayerGame(
            @Parameter(description = "게임 방 ID", required = true)
            @PathVariable("gameRoomId") Long gameRoomId,
            @CurrentMember Member member,
            @RequestBody @Valid MultiGameRequest.Start request) {
        
        // 경로의 gameRoomId와 요청 body의 gameRoomId 일치 확인
        validateGameRoomId(gameRoomId, request.getGameRoomId());
        
        return ApiResponseDto.onSuccess(
            startRoadViewSoloRoundUseCase.execute(member, request)
        );
    }

    @Operation(
        summary = "멀티 로드뷰 개인 정답 제출",
        description = "현재 라운드에 개인 정답을 제출합니다."
    )
    @PostMapping("/games/{gameId}/rounds/{roundId}/submissions")
    public ApiResponseDto<Void> submitPlayerAnswer(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId,
            @RequestBody @Valid SubmissionRequest.RoadViewPlayer request) {
        
        submitRoadViewPlayerAnswerUseCase.execute(roundId, request);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(
        summary = "멀티 로드뷰 개인 라운드 종료",
        description = "현재 라운드를 종료하고 결과를 반환합니다."
    )
    @PostMapping("/games/{gameId}/rounds/{roundId}/end")
    public ApiResponseDto<RoadViewRoundResponse.PlayerResult> endPlayerRound(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId,
            @PathVariable("roundId") Long roundId) {
        
        return ApiResponseDto.onSuccess(
            endRoadViewSoloRoundUseCase.execute(gameId, roundId)
        );
    }

    @Operation(
        summary = "멀티 로드뷰 다음 라운드 시작",
        description = "다음 라운드를 시작합니다. 마지막 라운드인 경우 게임이 종료됩니다."
    )
    @PostMapping("/games/{gameId}/rounds/next")
    public ApiResponseDto<MultiRoadViewGameResponse.NextRound> nextRound(
            @PathVariable("gameRoomId") Long gameRoomId,
            @PathVariable("gameId") Long gameId) {
        
        return ApiResponseDto.onSuccess(
            nextRoadViewRoundUseCase.execute(gameRoomId, gameId)
        );
    }

    /**
     * 경로의 gameRoomId와 요청 body의 gameRoomId 일치 확인
     */
    private void validateGameRoomId(Long pathGameRoomId, Long requestGameRoomId) {
        if (!pathGameRoomId.equals(requestGameRoomId)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_ID_MISMATCH);
        }
    }
}
```

---

## 📊 개선 전후 비교

### Before

```java
// TimeLimit을 매번 전달
RoadViewGameRound round1 = roundService.createGameRound(game, 1, 30, playerIds);
RoadViewGameRound round2 = roundService.createGameRound(game, 2, 30, playerIds);
RoadViewGameRound round3 = roundService.createGameRound(game, 3, 30, playerIds);

// API 경로
POST /multiRoadView/player/start
POST /multiRoadView/{multiGameId}/rounds/{roundId}/endPlayerRound
POST /multiRoadView/{multiGameId}/rounds/nextRound/next
```

### After

```java
// Game에서 자동으로 timeLimit 참조
RoadViewGameRound round1 = roundService.createGameRound(game, 1, playerIds);
RoadViewGameRound round2 = roundService.createGameRound(game, 2, playerIds);
RoadViewGameRound round3 = roundService.createGameRound(game, 3, playerIds);

// API 경로 - RESTful 계층 구조
POST /api/v1/game-rooms/{gameRoomId}/roadview/games/start
POST /api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/{roundId}/end
POST /api/v1/game-rooms/{gameRoomId}/roadview/games/{gameId}/rounds/next
```

---

## 🎯 핵심 개선 사항 요약

1. **단일 진실 공급원**: TimeLimit를 Game 레벨에서만 관리
2. **데이터 일관성**: 모든 라운드가 동일한 timeLimit 참조
3. **코드 간결성**: 라운드 생성 시 timeLimit 전달 불필요
4. **RESTful 설계**: 명확한 리소스 계층 구조
5. **검증 강화**: 경로 파라미터와 요청 body 일치 확인
6. **로깅 개선**: 주요 이벤트에 대한 상세 로그

