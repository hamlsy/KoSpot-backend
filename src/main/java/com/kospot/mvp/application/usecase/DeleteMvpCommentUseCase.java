package com.kospot.mvp.application.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.exception.object.domain.MvpHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.mvp.application.adaptor.MvpCommentAdaptor;
import com.kospot.mvp.application.service.MvpCommentService;
import com.kospot.mvp.domain.entity.MvpComment;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class DeleteMvpCommentUseCase {

    private final MvpCommentAdaptor mvpCommentAdaptor;
    private final MvpCommentService mvpCommentService;
    private final MemberAdaptor memberAdaptor;

    public void execute(Long memberId, Long commentId) {
        MvpComment comment = mvpCommentAdaptor.queryById(commentId);
        if (!comment.getMemberId().equals(memberId)) {
            Member member = memberAdaptor.queryById(memberId);
            if (!member.isAdmin()) {
                throw new MvpHandler(ErrorStatus.MVP_COMMENT_NO_PERMISSION);
            }
        }
        mvpCommentService.delete(comment);
    }
}
