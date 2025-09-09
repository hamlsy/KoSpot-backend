package com.kospot.domain.multigame.gamePlayer.exception;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class GameTeamHandler extends GeneralException {
    public GameTeamHandler(BaseCode code) {
        super(code);
    }
}
