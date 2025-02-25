package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.member.entity.Member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PointService {

    public void addPoint(Member member, int amount) {
        member.addPoint(amount);
    }

    public void usePoint(Member member, int amount) {
        member.usePoint(amount);
    }

}
