package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistory;
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
    public void addPoint(Member member, int amount, String description) {
        member.addPoint(amount);
        savePointHistory(member, amount, description);
    }

    @Override
    public void usePoint(Member member, int amount, String description) {
        member.usePoint(amount);
        savePointHistory(member, amount, description);
    }

    private void savePointHistory(Member member, int amount, String description){
        repository.save(
                PointHistory.create(member, amount, description)
        );
    }

}
