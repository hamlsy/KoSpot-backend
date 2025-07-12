package com.kospot.domain.point.service;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.adaptor.PointHistoryAdaptor;
import com.kospot.presentation.point.dto.response.PointHistoryResponse;
import com.kospot.domain.point.entity.PointHistory;
import com.kospot.domain.point.vo.PointHistoryType;
import com.kospot.domain.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointHistoryService {

    private final PointHistoryRepository repository;
    private final PointHistoryAdaptor adaptor;

    //todo change to usecase
    public List<PointHistoryResponse> findAllHistoryByMemberId(Long memberId) {
        List<PointHistory> histories = adaptor.queryAllHistoryByMemberId(memberId);
        return histories.stream()
                .map(PointHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void savePointHistory(Member member, int amount, PointHistoryType pointHistoryType){
        repository.save(
                PointHistory.create(member, amount, pointHistoryType)
        );
    }


}
