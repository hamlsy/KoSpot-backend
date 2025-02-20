package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.entity.PointHistoryType;

public interface PointService {
    void addPoint(Member member, int amount, PointHistoryType pointHistoryType);
    void usePoint(Member member, int amount, PointHistoryType pointHistoryType);
}
