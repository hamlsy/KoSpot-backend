package com.kospot.kospot.domain.point.service;

import com.kospot.kospot.domain.point.dto.response.PointHistoryResponse;

import java.util.List;

public interface PointHistoryService {

    List<PointHistoryResponse> findAllHistoryByMemberId(Long memberId);

}
