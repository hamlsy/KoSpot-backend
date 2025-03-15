package com.kospot.kospot.exception.object.domain;

import com.kospot.kospot.exception.object.general.GeneralException;
import com.kospot.kospot.exception.payload.code.BaseCode;

public class MemberItemHandler extends GeneralException {
    public MemberItemHandler(BaseCode code) {
        super(code);
    }
}
