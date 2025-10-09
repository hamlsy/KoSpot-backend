package com.kospot.application.multi.submission;

import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multi.submission.dto.request.SubmissionRequest;
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

    public void execute(Long roundId, SubmissionRequest.RoadViewPlayer request) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        GamePlayer gamePlayer = gamePlayerAdaptor.queryById(request.getPlayerId());

        RoadViewPlayerSubmission submission = request.toEntity();
        roadViewPlayerSubmissionService.createSubmission(round, gamePlayer, submission);
    }

}
