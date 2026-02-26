package com.kospot.infrastructure.websocket.subscription.impl;

import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.notification.constants.NotificationChannelConstants;
import com.kospot.infrastructure.websocket.subscription.SubscriptionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 개인 알림 채널 구독 검증자
 * - /user/{memberId}/notification 구독은 본인만 허용
 */
@Slf4j
@Component
public class PersonalNotificationSubscriptionValidator implements SubscriptionValidator {

    private static final String PERSONAL_NOTIFICATION_SUFFIX = "/notification";

    @Override
    public boolean canSubscribe(WebSocketMemberPrincipal principal, String destination) {
        if (principal == null || principal.getMemberId() == null || principal.getMemberId() <= 0) {
            log.warn("Personal notification access denied - No authentication");
            return false;
        }

        Long requestedMemberId = NotificationChannelConstants.extractMemberIdFromDestination(destination);
        if (requestedMemberId == null) {
            log.warn("Personal notification access denied - Invalid memberId in destination: {}", destination);
            return false;
        }

        boolean isOwner = requestedMemberId.equals(principal.getMemberId());
        if (!isOwner) {
            log.warn("Personal notification access denied - Not owner: PrincipalMemberId={}, DestinationMemberId={}",
                    principal.getMemberId(), requestedMemberId);
            return false;
        }

        return true;
    }

    @Override
    public boolean supports(String destination) {
        if (destination == null) {
            return false;
        }

        if (!destination.startsWith("/user/")) {
            return false;
        }

        return destination.endsWith(PERSONAL_NOTIFICATION_SUFFIX);
    }

    @Override
    public int getPriority() {
        return 300;
    }
}
