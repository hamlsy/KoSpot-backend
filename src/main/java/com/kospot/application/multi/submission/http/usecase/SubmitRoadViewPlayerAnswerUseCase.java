package com.kospot.application.multi.submission.http.usecase;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.domain.multi.submission.event.PlayerSubmissionCompletedEvent;
import com.kospot.domain.multi.submission.service.RoadViewSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.infrastructure.websocket.domain.multi.submission.service.SubmissionNotificationService;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class SubmitRoadViewPlayerAnswerUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final RoadViewSubmissionService roadViewSubmissionService;
    private final SubmissionNotificationService submissionNotificationService;
    private final SubmissionRedisService submissionRedisService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(Member member, String roomId, Long gameId,
                        Long roundId, SubmitRoadViewRequest.Player request) {
        // 1. 엔티티 조회
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        GamePlayer player = gamePlayerAdaptor.queryByMemberId(member.getId());

        // 2. 제출 저장 (DB)
        RoadViewSubmission submission = request.toEntity();
        roadViewSubmissionService.createPlayerSubmission(round, player, submission);

        // 3. Redis 카운터 업데이트
        Long currentCount = submissionRedisService.recordPlayerSubmission(
                GameMode.ROADVIEW,
                roundId,
                player.getId()
        );
        log.info("📝 Submission recorded - RoomId: {}, RoundId: {}, PlayerId: {}, Count: {}", 
                roomId, roundId, player.getId(), currentCount);

        // 4. WebSocket 알림
        submissionNotificationService.notifySubmissionReceived(gameId, roundId, player.getId());

        // 5. 제출 완료 이벤트 발행 (비동기 조기 종료 체크)
        eventPublisher.publishEvent(new PlayerSubmissionCompletedEvent(
                roomId,
                GameMode.ROADVIEW,
                gameId,
                roundId
        ));
    }

}
