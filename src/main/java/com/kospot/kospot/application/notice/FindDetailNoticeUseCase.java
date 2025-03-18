package com.kospot.kospot.application.notice;

import com.kospot.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.notice.dto.response.NoticeResponse;
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
