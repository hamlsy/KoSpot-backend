package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class CoordinateHandler extends GeneralException {
    public CoordinateHandler(BaseCode code) {
        super(code);
    }
}
