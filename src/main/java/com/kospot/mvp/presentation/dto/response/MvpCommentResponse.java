package com.kospot.mvp.presentation.dto.response;

import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.mvp.domain.entity.MvpComment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class MvpCommentResponse {

    @Getter
    @AllArgsConstructor
    public static class CommentInfo {
        private Long commentId;
        private String nickname;
        private String markerImageUrl;
        private String content;
        private LocalDateTime createdDate;

        public static CommentInfo from(MvpComment comment, MemberProfileRedisAdaptor.MemberProfileView profile) {
            return new CommentInfo(
                    comment.getId(),
                    profile.nickname(),
                    profile.markerImageUrl(),
                    comment.getContent(),
                    comment.getCreatedDate()
            );
        }
    }

    @Getter
    @AllArgsConstructor
    public static class CommentPage {
        private List<CommentInfo> comments;
        private int currentPage;
        private int totalPages;
        private long totalElements;
        private boolean hasNext;

        public static CommentPage from(Page<CommentInfo> page) {
            return new CommentPage(
                    page.getContent(),
                    page.getNumber(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.hasNext()
            );
        }
    }
}
