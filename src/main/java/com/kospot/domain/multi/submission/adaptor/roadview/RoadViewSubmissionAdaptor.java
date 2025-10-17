package com.kospot.domain.multi.submission.adaptor.roadview;

import com.kospot.domain.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadViewSubmissionAdaptor {

    private final RoadViewSubmissionRepository repository;

    public long queryCountByRoundId(Long roundId) {
        return repository.countByRoundId(roundId);
    }

    public List<RoadViewSubmission> queryByRoundId(Long roundId) {
        return repository.findByRoundId(roundId);
    }

}
