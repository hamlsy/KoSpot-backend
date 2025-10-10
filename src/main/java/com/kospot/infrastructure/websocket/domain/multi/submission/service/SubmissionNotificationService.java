package com.kospot.infrastructure.websocket.domain.multi.submission.service;

import com.kospot.application.multi.submission.websocket.message.PlayerSubmissionMessage;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubmissionNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifySubmissionReceived(Long gameId, Long roundId, Long playerId) {
        String topic = MultiGameChannelConstants.getRoadViewPlayerSubmissionChannel(gameId.toString());
        PlayerSubmissionMessage message = PlayerSubmissionMessage.builder()
                .playerId(playerId)
                .roundId(roundId)
                .timestamp(Instant.now())
                .build();
        messagingTemplate.convertAndSend(
                topic, message
        );
    }

}
