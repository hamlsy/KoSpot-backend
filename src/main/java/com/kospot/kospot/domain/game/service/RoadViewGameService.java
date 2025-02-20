package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.member.entity.Member;

public interface RoadViewGameService {

    StartGameResponse.RoadView startPracticeGame(String sidoKey);

    EndGameResponse.RoadViewPractice endPracticeGame(Member member, EndGameRequest.RoadView request);

    StartGameResponse.RoadView startRankGame();

    EndGameResponse.RoadViewRank endRankGame(Member member, EndGameRequest.RoadView request);
}
