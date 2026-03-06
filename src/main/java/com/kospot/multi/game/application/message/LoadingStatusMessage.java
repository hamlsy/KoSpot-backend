package com.kospot.multi.game.application.message;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoadingStatusMessage {
    private final List<MemberLoadingState> players;
    private final boolean allArrived;

    @Getter
    @Builder
    public static class MemberLoadingState {
        private final Long memberId;
        private final boolean arrived;
        private final Long acknowledgedAt;
    }
}

