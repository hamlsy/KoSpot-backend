package com.kospot.common.exception.payload.security;

import com.kospot.common.exception.payload.code.ErrorStatus;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {
    public JwtAuthenticationException(ErrorStatus errorStatus){
        super(errorStatus.name());
    }
}