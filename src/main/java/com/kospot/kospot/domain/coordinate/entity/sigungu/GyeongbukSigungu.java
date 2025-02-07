package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GyeongbukSigungu implements Sigungu {

    GYEONGSAN_SI("경산시"),
    GYEONGJU_SI("경주시"),
    GORYEONG_GUN("고령군"),
    GUMI_SI("구미시"),
    GUNWI_GUN("군위군"),
    GIMCHEON_SI("김천시"),
    MUNGYEONG_SI("문경시"),
    BONGHWA_GUN("봉화군"),
    SANGJU_SI("상주시"),
    SEONGJU_GUN("성주군"),
    ANDONG_SI("안동시"),
    YEONGDEOK_GUN("영덕군"),
    YEONGYANG_GUN("영양군"),
    YEONGJU_SI("영주시"),
    YEONGCHEON_SI("영천시"),
    YECHEON_GUN("예천군"),
    ULLEUNG_GUN("울릉군"),
    ULJIN_GUN("울진군"),
    UISEONG_GUN("의성군"),
    CHEONGDO_GUN("청도군"),
    CHEONGSONG_GUN("청송군"),
    CHILGOK_GUN("칠곡군"),
    POHANG_SI("포항시"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.GYEONGBUK;
    }

}
