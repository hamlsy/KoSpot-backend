package com.kospot.application.multi.submission.http.usecase;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.player.application.adaptor.GamePlayerAdaptor;
import com.kospot.multi.player.domain.entity.GamePlayer;
import com.kospot.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.multi.submission.event.PlayerSubmissionCompletedEvent;
import com.kospot.multi.submission.service.RoadViewSubmissionService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.redis.domain.multi.submission.service.SubmissionRedisService;
import com.kospot.common.websocket.domain.multi.submission.service.SubmissionNotificationService;
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

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final RoadViewSubmissionService roadViewSubmissionService;
    private final SubmissionNotificationService submissionNotificationService;
    private final SubmissionRedisService submissionRedisService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(Long memberId, String roomId, Long gameId,
                        Long roundId, SubmitRoadViewRequest.Player request) {
        Member member = memberAdaptor.queryById(memberId);
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchCoordinate(roundId);
        GamePlayer player = gamePlayerAdaptor.queryByMemberIdAndGameId(member.getId(), gameId);

        RoadViewSubmission submission = request.toEntity();
        roadViewSubmissionService.createPlayerSubmission(round, player, submission);

        Long currentCount = submissionRedisService.recordPlayerSubmission(
                GameMode.ROADVIEW,
                roundId,
                player.getId()
        );
        log.info("Submission recorded - RoomId: {}, RoundId: {}, PlayerId: {}, Count: {}",
                roomId, roundId, player.getId(), currentCount);

        submissionNotificationService.notifySubmissionReceived(gameId, roundId, player.getId());

        eventPublisher.publishEvent(new PlayerSubmissionCompletedEvent(
                roomId,
                gameId,
                roundId,
                GameMode.ROADVIEW,
                round.getMultiRoadViewGame().getMatchType()
        ));
    }

}
