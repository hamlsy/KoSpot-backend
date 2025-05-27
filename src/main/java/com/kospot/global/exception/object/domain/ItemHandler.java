package com.kospot.global.exception.object.domain;

import com.kospot.global.exception.object.general.GeneralException;
import com.kospot.global.exception.payload.code.BaseCode;

public class ItemHandler extends GeneralException {
    public ItemHandler(BaseCode code) {
        super(code);
    }
}
