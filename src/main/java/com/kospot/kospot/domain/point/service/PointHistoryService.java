package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.point.dto.response.PointHistoryResponse;
import com.kospot.kospot.domain.point.entity.PointHistoryType;

import java.util.List;

public interface PointHistoryService {

    List<PointHistoryResponse> findAllHistoryByMemberId(Long memberId);

    void savePointHistory(Member member, int amount, PointHistoryType pointHistoryType);

}
