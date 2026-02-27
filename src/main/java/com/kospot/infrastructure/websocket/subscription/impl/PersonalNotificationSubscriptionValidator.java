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

    @Override
    public boolean canSubscribe(WebSocketMemberPrincipal principal, String destination) {
        if (principal == null || principal.getMemberId() == null || principal.getMemberId() <= 0) {
            log.warn("Personal notification access denied - No authentication");
            return false;
        }

        // user-destination 구독은 본인 세션에만 바인딩되므로, 인증만 있으면 허용
        return true;
    }

    @Override
    public boolean supports(String destination) {
        if (destination == null) {
            return false;
        }

        return NotificationChannelConstants.PERSONAL_NOTIFICATION_SUBSCRIBE_CHANNEL.equals(destination);
    }

    @Override
    public int getPriority() {
        return 300;
    }
}
