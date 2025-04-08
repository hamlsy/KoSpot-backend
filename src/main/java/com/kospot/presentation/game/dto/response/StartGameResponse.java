package com.kospot.presentation.game.dto.response;

import lombok.Builder;
import lombok.Getter;

public class StartGameResponse {

    @Getter
    @Builder
    public static class RoadView {

        private String gameId;
        private String targetLat;
        private String targetLng;
        private String markerImageUrl;
    }

}
