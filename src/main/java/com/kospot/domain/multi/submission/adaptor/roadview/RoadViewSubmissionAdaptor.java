package com.kospot.domain.multi.submission.adaptor.roadview;

import com.kospot.domain.multi.submission.repository.RoadViewSubmissionRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadViewSubmissionAdaptor {

    private final RoadViewSubmissionRepository repository;

}
