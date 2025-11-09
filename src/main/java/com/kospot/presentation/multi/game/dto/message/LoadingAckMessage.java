package com.kospot.presentation.multi.game.dto.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoadingAckMessage {
    private Long roundId;
    private Long clientTimestamp;
}

