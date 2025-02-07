package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class CoordinateIdCacheHandler extends GeneralException {
    public CoordinateIdCacheHandler(BaseCode code) {
        super(code);
    }
}
