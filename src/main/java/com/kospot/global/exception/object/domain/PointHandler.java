package com.kospot.global.exception.object.domain;

import com.kospot.global.exception.object.general.GeneralException;
import com.kospot.global.exception.payload.code.BaseCode;

public class PointHandler extends GeneralException {
    public PointHandler(BaseCode code) {
        super(code);
    }
}
