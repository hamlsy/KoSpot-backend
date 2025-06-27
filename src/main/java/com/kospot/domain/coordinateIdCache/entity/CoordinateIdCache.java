package com.kospot.domain.coordinateIdCache.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.vo.Sido;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CoordinateIdCache extends BaseTimeEntity {

    @Id
    @Enumerated(EnumType.STRING)
    private Sido sido;

    @Column(nullable = false)
    private Long maxId;

    public void updateMaxId(Long maxId){
        this.maxId = maxId;
    }

}
