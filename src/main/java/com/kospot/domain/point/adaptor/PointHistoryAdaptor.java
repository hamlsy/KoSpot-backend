package com.kospot.domain.point.adaptor;


import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.entity.PointHistory;
import com.kospot.domain.point.repository.PointHistoryRepository;
import com.kospot.global.annotation.adaptor.Adaptor;
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
