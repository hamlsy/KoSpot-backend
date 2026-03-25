package com.kospot.mvp.application.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.mvp.application.adaptor.DailyMvpAdaptor;
import com.kospot.mvp.application.adaptor.MvpCommentAdaptor;
import com.kospot.mvp.domain.entity.MvpComment;
import com.kospot.mvp.presentation.response.MvpCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetMvpCommentsUseCase {

    private static final int PAGE_SIZE = 8;

    private final DailyMvpAdaptor dailyMvpAdaptor;
    private final MvpCommentAdaptor mvpCommentAdaptor;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public MvpCommentResponse.CommentPage execute(Long dailyMvpId, int page) {
        dailyMvpAdaptor.queryById(dailyMvpId);

        Pageable pageable = Pageable.ofSize(PAGE_SIZE).withPage(page);
        Page<MvpComment> commentPage = mvpCommentAdaptor.queryPageByDailyMvpId(dailyMvpId, pageable);

        Page<MvpCommentResponse.CommentInfo> infoPage = commentPage.map(comment -> {
            MemberProfileRedisAdaptor.MemberProfileView profile =
                    memberProfileRedisAdaptor.findProfile(comment.getMemberId());
            return MvpCommentResponse.CommentInfo.from(comment, profile);
        });

        return MvpCommentResponse.CommentPage.from(infoPage);
    }
}
