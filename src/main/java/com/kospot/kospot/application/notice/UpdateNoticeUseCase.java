package com.kospot.kospot.application.notice;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.kospot.domain.notice.entity.Notice;
import com.kospot.kospot.domain.notice.service.NoticeService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateNoticeUseCase {

    private final NoticeAdaptor noticeAdaptor;
    private final NoticeService noticeService;

    public void execute(Member member, Long noticeId, NoticeRequest.Update request) {
        member.validateAdmin();
        Notice notice = noticeAdaptor.findByIdFetchImage(noticeId);
        noticeService.updateNotice(notice, request);
    }

}
