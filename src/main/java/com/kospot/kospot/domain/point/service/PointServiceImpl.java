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

    @Override
    public void addPoint(Member member, int amount) {
        member.addPoint(amount);
    }

    @Override
    public void usePoint(Member member, int amount) {
        member.usePoint(amount);
    }

}
