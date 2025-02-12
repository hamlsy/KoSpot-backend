package com.kospot.kospot.domain.coordinateIdCache.entity;

import com.kospot.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
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
