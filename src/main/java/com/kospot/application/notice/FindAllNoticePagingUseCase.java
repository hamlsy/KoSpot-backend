package com.kospot.application.notice;

import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
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

    private final NoticeAdaptor noticeAdaptor;

    private static final int SIZE = 10;
    private static final String SORT_PROPERTIES = "createdDate";

    public List<NoticeResponse.Summary> execute(int page) {
        Pageable pageable = PageRequest.of(page, SIZE, Sort.Direction.DESC, SORT_PROPERTIES);
        Page<Notice> notices = noticeAdaptor.findAllPaging(pageable);
        return notices.stream().map(NoticeResponse.Summary::from)
                .collect(Collectors.toList());
    }

}
