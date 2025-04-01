package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class PointHandler extends GeneralException {
    public PointHandler(BaseCode code) {
        super(code);
    }
}
