package com.kospot.multi.game.presentation.payload;

import com.kospot.multi.round.presentation.dto.response.RoadViewRoundResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoadViewStartPayload {
    private final RoadViewRoundResponse.Info roundInfo;
}

