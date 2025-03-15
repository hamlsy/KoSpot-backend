package com.kospot.kospot.presentation.game.dto.response;

import com.kospot.kospot.domain.game.entity.RoadViewGame;
import lombok.Builder;
import lombok.Getter;

public class StartGameResponse {

    @Getter
    @Builder
    public static class RoadView {

        private String gameId;
        private String targetLat;
        private String targetLng;

    }

}
