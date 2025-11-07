package com.kospot.infrastructure.exception.object.domain;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class StatisticHandler extends GeneralException {
    public StatisticHandler(BaseCode code) {
        super(code);
    }
}

