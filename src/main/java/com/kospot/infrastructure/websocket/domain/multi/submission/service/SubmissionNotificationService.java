package com.kospot.infrastructure.websocket.domain.multi.submission.service;

import com.kospot.application.multi.submission.websocket.message.PlayerSubmissionMessage;
import com.kospot.infrastructure.websocket.domain.multi.submission.constants.SubmissionChannelConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifySubmissionReceived(String gameRoomId, Long playerId) {
        String topic = SubmissionChannelConstants.getRoadViewPlayerChannel(gameRoomId);
        PlayerSubmissionMessage message = PlayerSubmissionMessage.builder()
                .playerId(playerId)
                .build();
        messagingTemplate.convertAndSend(
                topic, message
        );
    }

}
