package com.kospot.infrastructure.websocket.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.security.Principal;

@Data
@AllArgsConstructor
public class ChatMemberPrincipal implements Principal {
    private final Long memberId;
    private final String nickname;
    private final String email;
    private final String role;

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
