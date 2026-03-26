package com.kospot.mvp.application.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.application.service.MvpCommentService;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.entity.MvpComment;
import com.kospot.mvp.presentation.dto.response.MvpCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class CreateMvpCommentUseCase {

    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final MvpCommentService mvpCommentService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public MvpCommentResponse.CommentInfo execute(Long memberId, Long dailyMvpId, String content) {
        DailyMvp dailyMvp = dailyMvpAdaptor.queryById(dailyMvpId);
        MvpComment comment = MvpComment.create(dailyMvp, memberId, content);
        MvpComment saved = mvpCommentService.save(comment);
        MemberProfileRedisAdaptor.MemberProfileView profile = memberProfileRedisAdaptor.findProfile(memberId);
        return MvpCommentResponse.CommentInfo.from(saved, profile);
    }
}
