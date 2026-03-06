package com.kospot.notice.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.notice.domain.entity.Notice;
import com.kospot.notice.domain.event.NoticeCreatedEvent;
import com.kospot.notice.application.service.NoticeImageAttachService;
import com.kospot.notice.application.service.NoticeService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.notice.service.RecentNoticeCacheService;
import com.kospot.notice.presentation.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateNoticeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final NoticeImageAttachService noticeImageAttachService;
    private final NoticeService noticeService;
    private final RecentNoticeCacheService recentNoticeCacheService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(Long memberId, NoticeRequest.Create request) {
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        Notice notice = noticeService.createNotice(request);
        noticeImageAttachService.attachImagesFromContent(notice, request.getContentMd());

        // 캐시 무효화
        recentNoticeCacheService.evictCache();

        // 공지사항 알림 이벤트 발행 (커밋 이후 리스너에서 알림 생성/푸시)
        eventPublisher.publishEvent(new NoticeCreatedEvent(
                notice.getId(),
                notice.getTitle(),
                notice.getCreatedDate()
        ));
    }

}
