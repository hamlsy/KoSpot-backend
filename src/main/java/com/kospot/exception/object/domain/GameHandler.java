package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class GameHandler extends GeneralException {
    public GameHandler(BaseCode code) {
        super(code);
    }
}
