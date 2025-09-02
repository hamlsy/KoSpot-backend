package com.kospot.infrastructure.websocket.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;

@Data
@AllArgsConstructor
public class WebSocketMemberPrincipal implements Principal {
    private final Long memberId;
    private final String nickname;
    private final String email;
    private final String role;

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }

    public static WebSocketMemberPrincipal getPrincipal(SimpMessageHeaderAccessor headerAccessor) {
        return (WebSocketMemberPrincipal) headerAccessor.getSessionAttributes().get("user");
    }
}
