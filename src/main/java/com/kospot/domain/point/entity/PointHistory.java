package com.kospot.domain.point.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.point.vo.PointHistoryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PointHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int changeAmount;

    @Enumerated(EnumType.STRING)
    private PointHistoryType pointHistoryType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public static PointHistory create(Member member, int changeAmount, PointHistoryType pointHistoryType) {
        return PointHistory.builder()
                .changeAmount(changeAmount)
                .pointHistoryType(pointHistoryType)
                .member(member)
                .build();
    }

}
