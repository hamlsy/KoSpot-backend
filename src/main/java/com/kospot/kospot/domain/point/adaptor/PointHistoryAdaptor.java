package com.kospot.kospot.domain.point.adaptor;


import com.kospot.kospot.domain.point.entity.PointHistory;
import com.kospot.kospot.domain.point.repository.PointHistoryRepository;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointHistoryAdaptor {

    private final PointHistoryRepository repository;

    public List<PointHistory> queryAllHistoryByMemberId(Long memberId) {
        return repository.findAllByMemberId(memberId);
    }

}
