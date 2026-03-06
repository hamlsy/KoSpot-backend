package com.kospot.multi.player.domain.exception;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class GameTeamHandler extends GeneralException {
    public GameTeamHandler(BaseCode code) {
        super(code);
    }
}
