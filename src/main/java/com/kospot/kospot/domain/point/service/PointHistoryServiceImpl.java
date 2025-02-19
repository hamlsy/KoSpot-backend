package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.kospot.domain.point.dto.response.PointHistoryResponse;
import com.kospot.kospot.domain.point.entity.PointHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointHistoryServiceImpl implements PointHistoryService{

    private final PointHistoryAdaptor adaptor;

    @Override
    public List<PointHistoryResponse> findAllHistoryByMemberId(Long memberId) {
        List<PointHistory> histories = adaptor.queryAllHistoryByMemberId(memberId);
        return histories.stream()
                .map(PointHistoryResponse::from)
                .collect(Collectors.toList());
    }


}
