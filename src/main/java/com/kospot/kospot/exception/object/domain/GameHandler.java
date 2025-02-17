package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class GameHandler extends GeneralException {
    public GameHandler(BaseCode code) {
        super(code);
    }
}
