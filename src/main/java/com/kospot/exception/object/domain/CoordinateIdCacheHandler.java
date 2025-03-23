package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class CoordinateIdCacheHandler extends GeneralException {
    public CoordinateIdCacheHandler(BaseCode code) {
        super(code);
    }
}
