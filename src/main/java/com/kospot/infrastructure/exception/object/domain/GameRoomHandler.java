package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class GameRoomHandler extends GeneralException {
    public GameRoomHandler(BaseCode code) {
        super(code);
    }
}
