package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class CoordinateHandler extends GeneralException {
    public CoordinateHandler(BaseCode code) {
        super(code);
    }
}
