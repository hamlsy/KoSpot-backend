package com.kospot.application.multi.submission.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadview.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.submission.service.SubmissionNotificationService;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class SubmitRoadViewPlayerAnswerUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final RoadViewPlayerSubmissionService roadViewPlayerSubmissionService;
    private final SubmissionNotificationService submissionNotificationService;

    public void execute(Member member, Long gameId,
                        Long roundId, SubmitRoadViewRequest.Player request) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);

        GamePlayer player = gamePlayerAdaptor.queryByMemberId(member.getId());

        RoadViewPlayerSubmission submission = request.toEntity();
        roadViewPlayerSubmissionService.createSubmission(round, player, submission);

        // notify
        submissionNotificationService.notifySubmissionReceived(gameId, roundId, player.getId());
    }


}
