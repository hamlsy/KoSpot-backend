package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class StatisticHandler extends GeneralException {
    public StatisticHandler(BaseCode code) {
        super(code);
    }
}

