package com.kospot.application.multiplayer.submission;

import com.kospot.domain.multigame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multigame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multigame.gameRound.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multigame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multigame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multigame.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multigame.submission.dto.request.SubmissionRequest;
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
