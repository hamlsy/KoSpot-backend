package com.kospot.presentation.chat.dto.request;

import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.chat.vo.MessageType;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ChatMessageDto {

    @NotEmpty
    private String content;
    private String messageType;
    private String channelType;

    private String roomId;
    private String teamId;

}
