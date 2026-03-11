package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class GameRoomHandler extends GeneralException {
    public GameRoomHandler(BaseCode code) {
        super(code);
    }
}
