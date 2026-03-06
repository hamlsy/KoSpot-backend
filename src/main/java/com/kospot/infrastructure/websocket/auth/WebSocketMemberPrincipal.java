package com.kospot.infrastructure.websocket.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;
import java.util.Map;

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
        if (headerAccessor == null) {
            return null;
        }

        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return null;
        }

        Object user = sessionAttributes.get("user");
        if (user instanceof WebSocketMemberPrincipal principal) {
            return principal;
        }

        return null;
    }

}
