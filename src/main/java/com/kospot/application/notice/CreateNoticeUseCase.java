package com.kospot.application.notice;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.event.NoticeCreatedEvent;
import com.kospot.domain.notice.service.NoticeImageAttachService;
import com.kospot.domain.notice.service.NoticeService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.notice.service.RecentNoticeCacheService;
import com.kospot.presentation.notice.dto.request.NoticeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateNoticeUseCase {

    private final NoticeImageAttachService noticeImageAttachService;
    private final NoticeService noticeService;
    private final RecentNoticeCacheService recentNoticeCacheService;
    private final ApplicationEventPublisher eventPublisher;

    public void execute(Member member, NoticeRequest.Create request) {
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
