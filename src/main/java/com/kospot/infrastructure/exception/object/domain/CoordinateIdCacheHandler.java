package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class CoordinateIdCacheHandler extends GeneralException {
    public CoordinateIdCacheHandler(BaseCode code) {
        super(code);
    }
}
