package com.kospot.presentation.multi.game.strategy.payload;

import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoadViewStartPayload {
    private final RoadViewRoundResponse.Info roundInfo;
}

