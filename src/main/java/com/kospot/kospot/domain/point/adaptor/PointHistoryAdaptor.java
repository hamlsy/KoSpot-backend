package com.kospot.kospot.domain.point.adaptor;


import com.kospot.kospot.domain.point.entity.PointHistory;
import com.kospot.kospot.domain.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PointHistoryAdaptor {

    private final PointHistoryRepository repository;

    public List<PointHistory> queryAllHistoryByMemberId(Long memberId) {
        return repository.findAllByMemberId(memberId);
    }

}
