package com.kospot.presentation.test;

import com.kospot.domain.notification.event.FriendRequestCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@Profile("!prod")
@RequiredArgsConstructor
@RequestMapping("/api/test/notifications")
public class NotificationTestController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/friend-request")
    public ResponseEntity<Map<String, Object>> triggerFriendRequestNotification(
            @RequestParam("friendRequestId") Long friendRequestId,
            @RequestParam("senderMemberId") Long senderMemberId,
            @RequestParam("receiverMemberId") Long receiverMemberId
    ) {
        eventPublisher.publishEvent(new FriendRequestCreatedEvent(
                friendRequestId,
                senderMemberId,
                receiverMemberId,
                LocalDateTime.now()
        ));

        log.info("Friend request notification test triggered - FriendRequestId: {}", friendRequestId);

        return ResponseEntity.ok(Map.of(
                "status", "published",
                "friendRequestId", friendRequestId,
                "senderMemberId", senderMemberId,
                "receiverMemberId", receiverMemberId
        ));
    }
}
