package com.kospot.notice.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.notice.application.adaptor.NoticeAdaptor;
import com.kospot.notice.domain.entity.Notice;
import com.kospot.notice.application.service.NoticeService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.notice.service.RecentNoticeCacheService;
import com.kospot.notice.presentation.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateNoticeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final NoticeAdaptor noticeAdaptor;
    private final NoticeService noticeService;
    private final RecentNoticeCacheService recentNoticeCacheService;

    public void execute(Long memberId, Long noticeId, NoticeRequest.Update request) {
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        Notice notice = noticeAdaptor.findByIdFetchImage(noticeId);
        noticeService.updateNotice(notice, request);

        // 캐시 무효화
        recentNoticeCacheService.evictCache();
    }

}
