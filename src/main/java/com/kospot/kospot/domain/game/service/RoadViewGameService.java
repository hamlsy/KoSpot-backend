package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.game.dto.request.EndGameRequest;
import com.kospot.kospot.domain.game.dto.response.EndGameResponse;
import com.kospot.kospot.domain.game.dto.response.StartGameResponse;

public interface RoadViewGameService {

    StartGameResponse.RoadView startPracticeGame(String sidoKey);

    EndGameResponse.RoadViewPractice endPracticeGame(EndGameRequest.RoadViewPractice request);

}
