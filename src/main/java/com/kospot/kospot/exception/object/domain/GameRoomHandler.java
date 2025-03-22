package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class GameRoomHandler extends GeneralException {
    public GameRoomHandler(BaseCode code) {
        super(code);
    }
}
