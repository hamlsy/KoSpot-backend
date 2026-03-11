package com.kospot.notice.application.usecase;

import com.kospot.notice.application.adaptor.NoticeAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.notice.presentation.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetDetailNoticeUseCase {

    private final NoticeAdaptor noticeAdaptor;

    public NoticeResponse.Detail execute(Long id) {
        return NoticeResponse.Detail.from(noticeAdaptor.findById(id));
    }

    public NoticeResponse.Markdown executeMarkdownContent(Long id) {
        return NoticeResponse.Markdown.from(noticeAdaptor.findById(id));
    }

}
