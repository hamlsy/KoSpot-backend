package com.kospot.domain.notice.adaptor;

import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.notice.repository.NoticeRepository;
import com.kospot.global.exception.object.domain.NoticeHandler;
import com.kospot.global.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeAdaptor {

    private final NoticeRepository noticeRepository;

    public Page<Notice> findAllPaging(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    public Notice findById(Long id) {
        return noticeRepository.findById(id).orElseThrow(
        () -> new NoticeHandler(ErrorStatus.NOTICE_NOT_FOUND));
    }

    public Notice findByIdFetchImage(Long id) {
        return noticeRepository.findByIdFetchImage(id).orElseThrow(
                () -> new NoticeHandler(ErrorStatus.NOTICE_NOT_FOUND)
        );
    }

}
