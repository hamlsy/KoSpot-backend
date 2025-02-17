package com.kospot.kospot.domain.game.service;

import com.kospot.kospot.domain.game.dto.response.StartGameResponse;

public interface RoadViewGameService {

    StartGameResponse.RoadView startPracticeGame(String sidoKey);

    void endPracticeGame();

}
