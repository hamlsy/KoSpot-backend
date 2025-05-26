package com.kospot.global.exception.object.domain;

import com.kospot.global.exception.object.general.GeneralException;
import com.kospot.global.exception.payload.code.BaseCode;

public class MemberItemHandler extends GeneralException {
    public MemberItemHandler(BaseCode code) {
        super(code);
    }
}
