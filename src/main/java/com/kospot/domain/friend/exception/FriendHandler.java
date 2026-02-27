package com.kospot.domain.friend.exception;

import com.kospot.infrastructure.exception.object.general.GeneralException;
import com.kospot.infrastructure.exception.payload.code.BaseCode;

public class FriendHandler extends GeneralException {

    public FriendHandler(BaseCode code) {
        super(code);
    }
}
