package com.kospot.point.application.service;

import com.kospot.member.domain.entity.Member;
import com.kospot.point.application.adaptor.PointHistoryAdaptor;
import com.kospot.point.presentation.response.PointHistoryResponse;
import com.kospot.point.domain.entity.PointHistory;
import com.kospot.point.domain.vo.PointHistoryType;
import com.kospot.point.infrastructure.persistence.PointHistoryRepository;
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
