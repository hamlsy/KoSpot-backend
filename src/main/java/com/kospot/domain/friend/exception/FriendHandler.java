package com.kospot.domain.friend.exception;

import com.kospot.common.exception.object.general.GeneralException;
import com.kospot.common.exception.payload.code.BaseCode;

public class FriendHandler extends GeneralException {

    public FriendHandler(BaseCode code) {
        super(code);
    }
}
