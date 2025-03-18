package com.kospot.kospot.domain.notice.adaptor;

import com.kospot.kospot.domain.notice.entity.Notice;
import com.kospot.kospot.domain.notice.repository.NoticeRepository;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeAdaptor {

    private final NoticeRepository noticeRepository;

    public Page<Notice> findAllPaging(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

}
