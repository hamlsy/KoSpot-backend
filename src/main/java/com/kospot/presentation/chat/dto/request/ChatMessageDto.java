package com.kospot.presentation.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ChatMessageDto {

    @NotEmpty
    private String content;
    private String messageType;
    private String channelType;

    private Long roomId;
    private String teamId;
    private Long gamePlayerId;
    private Long gameRoomId;

}
