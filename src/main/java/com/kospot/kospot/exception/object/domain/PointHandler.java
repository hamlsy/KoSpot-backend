package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class PointHandler extends GeneralException {
    public PointHandler(BaseCode code) {
        super(code);
    }
}
