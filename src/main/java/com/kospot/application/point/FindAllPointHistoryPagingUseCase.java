package com.kospot.application.point;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.domain.point.entity.PointHistory;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.point.dto.response.PointHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class FindAllPointHistoryPagingUseCase {

    private final static int SIZE = 10;
    private final PointHistoryAdaptor pointHistoryAdaptor;

    public List<PointHistoryResponse> execute(Member member, int page) {
        Pageable pageable = PageRequest.of(page, SIZE, Sort.Direction.DESC, "createdDate");
        List<PointHistory> pointHistories = pointHistoryAdaptor.queryAllByMemberPaging(member, pageable);
        return pointHistories.stream().map(PointHistoryResponse::from)
                .collect(Collectors.toList());
    }

}
