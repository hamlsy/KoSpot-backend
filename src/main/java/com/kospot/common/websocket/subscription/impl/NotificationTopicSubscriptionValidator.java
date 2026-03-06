package com.kospot.common.websocket.subscription.impl;

import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.notification.infrastructure.websocket.constants.NotificationChannelConstants;
import com.kospot.common.websocket.subscription.SubscriptionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 알림 전역 채널 구독 검증자
 * - /topic/notification/** 구독은 기본적으로 허용
 */
@Slf4j
@Component
public class NotificationTopicSubscriptionValidator implements SubscriptionValidator {

    @Override
    public boolean canSubscribe(WebSocketMemberPrincipal principal, String destination) {
        // 전역 알림은 기본 허용 (로그인 여부와 무관)
        return true;
    }

    @Override
    public boolean supports(String destination) {
        if (destination == null) {
            return false;
        }
        return destination.startsWith(NotificationChannelConstants.PREFIX_NOTIFICATION);
    }

    @Override
    public int getPriority() {
        return 50;
    }
}
