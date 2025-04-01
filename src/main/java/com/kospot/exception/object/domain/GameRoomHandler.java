package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class GameRoomHandler extends GeneralException {
    public GameRoomHandler(BaseCode code) {
        super(code);
    }
}
