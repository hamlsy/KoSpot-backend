package com.kospot.multi.submission.application.roadview;

import com.kospot.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.multi.submission.infrastructure.persistence.RoadViewSubmissionRepository;
import com.kospot.common.annotation.adaptor.Adaptor;
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

    public List<RoadViewSubmission> queryByRoundIdFetchGamePlayer(Long roundId) {
        return repository.findByRoundIdFetchGamePlayer(roundId);
    }

}
