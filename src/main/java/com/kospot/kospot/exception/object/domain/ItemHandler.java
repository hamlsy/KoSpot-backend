package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.payload.code.BaseCode;

public class ItemHandler extends RuntimeException {
    public ItemHandler(BaseCode code) {
        super(code);
    }
}
