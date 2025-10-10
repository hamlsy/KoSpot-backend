package com.kospot.application.multi.submission.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.multi.submission.service.SubmissionNotificationService;
import com.kospot.presentation.multi.submission.dto.request.SubmitRoadViewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class SubmitRoadViewPlayerAnswerUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final RoadViewPlayerSubmissionService roadViewPlayerSubmissionService;
    private final SubmissionNotificationService submissionNotificationService;

    public void execute(Member member, Long gameRoomId, Long roundId, SubmitRoadViewRequest.Player request) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        GamePlayer player = gamePlayerAdaptor.queryById(request.getPlayerId());
        validateMemberMatchPlayer(member, player);

        RoadViewPlayerSubmission submission = request.toEntity();
        roadViewPlayerSubmissionService.createSubmission(round, player, submission);

        // notify
        submissionNotificationService.notifySubmissionReceived();
    }

    public void validateMemberMatchPlayer(Member member, GamePlayer player) {
        if(!Objects.equals(member.getId(), player.getMemberId())) {
            throw new IllegalArgumentException("Member does not match the player.");
        }
    }

}
