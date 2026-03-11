package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class GameHandler extends GeneralException {
    public GameHandler(BaseCode code) {
        super(code);
    }
}
