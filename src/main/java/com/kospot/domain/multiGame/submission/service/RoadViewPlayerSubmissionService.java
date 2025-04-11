package com.kospot.domain.multiGame.submission.service;

import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multiGame.submission.repository.RoadViewPlayerSubmissionRepository;
import com.kospot.presentation.multiGame.submission.dto.request.SubmissionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoadViewPlayerSubmissionService {

    private final RoadViewPlayerSubmissionRepository roadViewPlayerSubmissionRepository;

    public void createSubmission(RoadViewGameRound roadViewGameRound, GamePlayer gamePlayer, RoadViewPlayerSubmission submission) {
        submission.setGamePlayer(gamePlayer);
        submission.setRoadViewGameRound(roadViewGameRound);
        roadViewPlayerSubmissionRepository.save(submission);
    }

}
