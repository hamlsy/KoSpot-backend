package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GyeongnamSigungu implements Sigungu {

    GEOJE_SI("거제시"),
    GEOCHEONG_GUN("거창군"),
    GOSEONG_GUN("고성군"),
    GIMHAE_SI("김해시"),
    NAMHAE_GUN("남해군"),
    MIRYANG_SI("밀양시"),
    SACHEON_SI("사천시"),
    SANCHUNG_GUN("산청군"),
    YANGSAN_SI("양산시"),
    UIRYEONG_GUN("의령군"),
    JINJU_SI("진주시"),
    CHANGNYEONG_GUN("창녕군"),
    CHANGWON_SI("창원시"),
    TONGYEONG_SI("통영시"),
    HADONG_GUN("하동군"),
    HAMAN_GUN("함안군"),
    HAMYANG_GUN("함양군"),
    HAPCHEON_GUN("합천군"),
    ;

    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Sido getParentSido() {
        return Sido.GYEONGNAM;
    }

}
