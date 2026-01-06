package com.kospot.application.notice;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.service.NoticeService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.notice.service.RecentNoticeCacheService;
import com.kospot.presentation.notice.dto.request.NoticeRequest;
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
    private final RecentNoticeCacheService recentNoticeCacheService;

    public void execute(Member member, Long noticeId, NoticeRequest.Update request) {
        member.validateAdmin();
        Notice notice = noticeAdaptor.findByIdFetchImage(noticeId);
        noticeService.updateNotice(notice, request);

        // 캐시 무효화
        recentNoticeCacheService.evictCache();
    }

}
