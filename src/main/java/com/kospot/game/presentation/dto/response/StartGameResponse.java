package com.kospot.game.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

public class StartGameResponse {

    @Getter
    @Builder
    public static class RoadView {

        private Long gameId;
        private String poiName;
        private String targetLat;
        private String targetLng;
        private String markerImageUrl;
        private String practiceToken;
    }

    @Getter
    @Builder
    public static class ReIssue {
        private String poiName;
        private String targetLat;
        private String targetLng;
    }

}
