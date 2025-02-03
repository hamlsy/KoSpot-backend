package com.kospot.kospot.domain.coordinate.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    private String sido;     // 시도 (서울특별시, 경기도 등)
    private String sigungu;  // 시군구 (강남구, 수원시 등)
    private String eupmyeondong; // 읍면동/구 (삼성동, 영등포동 등)
    private String eupmyeonri;   // 읍/면/리 (농촌 지역 주소)

}
