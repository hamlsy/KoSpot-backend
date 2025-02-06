package com.kospot.kospot.domain.coordinate.entity;

import com.kospot.kospot.domain.coordinate.entity.region.converter.SigunguConverter;
import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.region.sigungu.Sigungu;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Address {

    @Enumerated(EnumType.STRING)
    private Sido sido;     // 시도 (서울특별시, 경기도 등)

    @Convert(converter = SigunguConverter.class)
    private Sigungu sigungu;  // 시군구 (강남구, 수원시 등)

//    @Convert(converter = EupmyeondongConverter.class)
//    private Eupmyeondong eupmyeondong; // 읍면동/구 (삼성동, 영등포동 등)

//    private String eupmyeonri;   // 읍/면/리 (농촌 지역 주소)

    private String detailAddress;
}
