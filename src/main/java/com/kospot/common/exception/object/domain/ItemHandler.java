package com.kospot.common.exception.object.domain;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class ItemHandler extends GeneralException {
    public ItemHandler(BaseCode code) {
        super(code);
    }
}
