package com.kospot.kospot.domain.member.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.exception.object.domain.PointHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String nickname;

    private String email;

    private int point;

    //business

    /**
     * Point
     */

    public void addPoint(int amount){
        this.point += amount;
    }

    public void usePoint(int amount){
        if(this.point < amount){
            throw new PointHandler(ErrorStatus.POINT_INSUFFICIENT);
        }
        this.point -= amount;
    }


}
