package com.kospot.kospot.application.notice;

import com.kospot.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.kospot.domain.notice.entity.Notice;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllNoticePagingUseCase {

    private final static int SIZE = 10;
    private final NoticeAdaptor noticeAdaptor;

    public List<NoticeResponse.Summary> execute(int page) {
        Pageable pageable = PageRequest.of(page, SIZE, Sort.Direction.DESC, "createdDate");
        Page<Notice> notices = noticeAdaptor.findAllPaging(pageable);
        return notices.stream().map(NoticeResponse.Summary::from)
                .collect(Collectors.toList());
    }

}
