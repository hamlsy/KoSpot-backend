package com.kospot.mvp.domain.entity;

import com.kospot.common.auditing.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "mvp_comment",
        indexes = {
                @Index(name = "idx_mvp_comment_daily_mvp_id", columnList = "daily_mvp_id"),
                @Index(name = "idx_mvp_comment_member_id", columnList = "member_id")
        }
)
public class MvpComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_mvp_id", nullable = false)
    private DailyMvp dailyMvp;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 300)
    private String content;

    public static MvpComment create(DailyMvp dailyMvp, Long memberId, String content) {
        return MvpComment.builder()
                .dailyMvp(dailyMvp)
                .memberId(memberId)
                .content(content)
                .build();
    }
}
