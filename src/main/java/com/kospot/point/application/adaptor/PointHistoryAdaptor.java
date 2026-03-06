package com.kospot.point.application.adaptor;


import com.kospot.member.domain.entity.Member;
import com.kospot.point.domain.entity.PointHistory;
import com.kospot.point.infrastructure.persistence.PointHistoryRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    public List<PointHistory> queryAllByMemberPaging(Member member, Pageable pageable) {
        return repository.findAllByMemberPaging(member, pageable);
    }


}
