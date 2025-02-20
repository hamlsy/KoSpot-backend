package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistory;
import com.kospot.kospot.domain.point.entity.PointHistoryType;
import com.kospot.kospot.domain.point.repository.PointHistoryRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    private final PointHistoryRepository repository;

    @Override
    public void addPoint(Member member, int amount, PointHistoryType pointHistoryType) {
        member.addPoint(amount);
        savePointHistory(member, amount, pointHistoryType);
    }

    @Override
    public void usePoint(Member member, int amount, PointHistoryType pointHistoryType) {
        member.usePoint(amount);
        savePointHistory(member, -1 * amount, pointHistoryType);
    }

    private void savePointHistory(Member member, int amount, PointHistoryType pointHistoryType){
        repository.save(
                PointHistory.create(member, amount, pointHistoryType)
        );
    }

}
