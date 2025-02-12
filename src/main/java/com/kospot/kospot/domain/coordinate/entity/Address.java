package com.kospot.kospot.domain.coordinate.entity;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Embeddable
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @Enumerated(EnumType.STRING)
    private Sido sido;     // 시도 (서울특별시, 경기도 등)

    private String sigungu;  // 시군구 (강남구, 수원시 등)

    private String detailAddress;
}
