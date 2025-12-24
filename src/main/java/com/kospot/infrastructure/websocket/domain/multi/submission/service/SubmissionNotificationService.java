package com.kospot.infrastructure.websocket.domain.multi.submission.service;

import com.kospot.application.multi.submission.websocket.message.PlayerSubmissionMessage;
import com.kospot.infrastructure.doc.annotation.WebSocketDoc;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubmissionNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @WebSocketDoc(
        trigger = "플레이어가 제출을 완료했을 때",
        description = "로드뷰 개인전 게임의 라운드에서 플레이어의 제출 완료 알림 메시지를 방송합니다.",
        destination = MultiGameChannelConstants.PREFIX_GAME + "{gameId}/roadview/submissions/player",
        payloadType = PlayerSubmissionMessage.class
    )
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
