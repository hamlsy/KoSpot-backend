package com.kospot.kospot.domain.coordinate.entity;

import com.kospot.kospot.domain.coordinate.entity.region.eupmyeondong.Eupmyeondong;
import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.region.sigungu.Sigungu;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    private Sigungu sigungu;  // 시군구 (강남구, 수원시 등)

    @Enumerated(EnumType.STRING)
    private Eupmyeondong eupmyeondong; // 읍면동/구 (삼성동, 영등포동 등)

    private String eupmyeonri;   // 읍/면/리 (농촌 지역 주소)

}
