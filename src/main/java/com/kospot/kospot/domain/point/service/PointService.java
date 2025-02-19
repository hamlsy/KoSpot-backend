package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.member.entity.Member;

public interface PointService {
    void addPoint(Member member, int amount, String description);
    void usePoint(Member member, int amount, String description);
}
