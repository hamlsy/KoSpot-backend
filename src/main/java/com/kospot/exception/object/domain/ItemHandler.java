package com.kospot.exception.object.domain;

import com.kospot.exception.object.general.GeneralException;
import com.kospot.exception.payload.code.BaseCode;

public class ItemHandler extends GeneralException {
    public ItemHandler(BaseCode code) {
        super(code);
    }
}
