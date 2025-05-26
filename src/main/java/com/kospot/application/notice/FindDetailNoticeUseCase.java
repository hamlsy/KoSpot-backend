package com.kospot.application.notice;

import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindDetailNoticeUseCase {

    private final NoticeAdaptor noticeAdaptor;

    public NoticeResponse.Detail execute(Long id) {
        return NoticeResponse.Detail.from(noticeAdaptor.findById(id));
    }

}
