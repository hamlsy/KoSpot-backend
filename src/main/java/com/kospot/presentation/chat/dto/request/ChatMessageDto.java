package com.kospot.presentation.chat.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {

    @NotEmpty
    private String content;

    private Long roomId;
    private String teamId;
    private Long gamePlayerId;
    private Long gameRoomId;

}
