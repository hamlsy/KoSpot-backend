package com.kospot.mvp.application.adaptor;

import com.kospot.common.annotation.adaptor.Adaptor;
import com.kospot.common.exception.object.domain.MvpHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.mvp.domain.entity.MvpComment;
import com.kospot.mvp.infrastructure.persistence.MvpCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MvpCommentAdaptor {

    private final MvpCommentRepository mvpCommentRepository;

    public Page<MvpComment> queryPageByDailyMvpId(Long dailyMvpId, Pageable pageable) {
        return mvpCommentRepository.findByDailyMvpIdOrderByCreatedDateDesc(dailyMvpId, pageable);
    }

    public MvpComment queryById(Long commentId) {
        return mvpCommentRepository.findById(commentId)
                .orElseThrow(() -> new MvpHandler(ErrorStatus.MVP_COMMENT_NOT_FOUND));
    }

    public long countByMvpDate(LocalDate mvpDate) {
        return mvpCommentRepository.countByDailyMvp_MvpDate(mvpDate);
    }
}
