package com.kospot.notice.application.usecase;

import com.kospot.notice.application.adaptor.NoticeAdaptor;
import com.kospot.notice.domain.entity.Notice;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.notice.presentation.dto.response.NoticeResponse;
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
