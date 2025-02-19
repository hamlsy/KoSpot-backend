package com.kospot.kospot.domain.point.adaptor;

import com.kospot.kospot.domain.point.entity.PointHistory;

import java.util.List;

public interface PointHistoryAdaptor {
    List<PointHistory> queryAllHistoryByMemberId(Long memberId);

}
