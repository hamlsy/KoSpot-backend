package com.kospot.application.multiGame.submission;

import com.kospot.domain.multiGame.game.adaptor.MultiRoadViewGameAdaptor;
import com.kospot.domain.multiGame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.submission.dto.request.SubmissionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class SubmitRoadViewPlayerAnswerUseCase {

    private final MultiRoadViewGameAdaptor multiRoadViewGameAdaptor;
    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final RoadViewPlayerSubmissionService roadViewPlayerSubmissionService;

    public void execute(Long roundId, SubmissionRequest.RoadViewPlayer request) {
        RoadViewGameRound gameRound = roadViewGameRoundAdaptor.queryById(roundId);
        GamePlayer gamePlayer = gamePlayerAdaptor.queryById(request.getPlayerId());
        roadViewPlayerSubmissionService.createSubmission(gameRound, gamePlayer, request);
    }

}
