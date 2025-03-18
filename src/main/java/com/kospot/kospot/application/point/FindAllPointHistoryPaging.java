package com.kospot.kospot.application.point;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.domain.point.entity.PointHistory;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.point.dto.response.PointHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Pageable;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllPointHistoryPaging {

    private final int SIZE = 15;
    private final PointHistoryAdaptor pointHistoryAdaptor;

    public List<PointHistoryResponse> execute(Member member, int page) {
        Pageable pageable = (Pageable) PageRequest.of(page, SIZE, Sort.Direction.DESC, "createdDate");
        List<PointHistory> pointHistories = pointHistoryAdaptor.queryAllByMemberIdPaging(member, pageable);
        return pointHistories.stream().map(PointHistoryResponse::from)
                .collect(Collectors.toList());
    }

}
