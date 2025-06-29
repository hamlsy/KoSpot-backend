package com.kospot.presentation.chat.dto.request;

import com.kospot.domain.chat.entity.ChatMessage;
import com.kospot.domain.chat.vo.ChannelType;
import com.kospot.domain.chat.vo.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ChatMessageDto {

    private String messageType;
    private String channelType;
    private String content;
    private String teamId;

}
