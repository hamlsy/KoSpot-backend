package com.kospot.domain.chat.service;

import com.kospot.domain.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {
    private final MessageRepository messageRepository;

    public MessageDto createMessage(CreateMessageRequest request) {

    }
}
