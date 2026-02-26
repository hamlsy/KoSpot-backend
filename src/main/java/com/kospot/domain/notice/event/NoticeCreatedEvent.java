package com.kospot.domain.notice.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NoticeCreatedEvent {

    private final Long noticeId;
    private final String title;
    private final LocalDateTime createdAt;
}
