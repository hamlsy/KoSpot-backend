package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GyeonggiSigungu implements Sigungu{

    GAPYEONG_GUN("가평군"),
    GOYANG_SI("고양시"),
    GWACHEON_SI("과천시"),
    GWANGMYEONG_SI("광명시"),
    GWANGJU_SI("광주시"),
    GURI_SI("구리시"),
    GUNPO_SI("군포시"),
    GIMPO_SI("김포시"),
    NAMYANGJU_SI("남양주시"),
    DONGDUCHEON_SI("동두천시"),
    BUCHEON_SI("부천시"),
    SEONGNAM_SI("성남시"),
    SUWON_SI("수원시"),
    SIHEUNG_SI("시흥시"),
    ANSAN_SI("안산시"),
    ANSEONG_SI("안성시"),
    ANYANG_SI("안양시"),
    YANGJU_SI("양주시"),
    YANGPYEONG_GUN("양평군"),
    YEOJU_SI("여주시"),
    YEONCHEON_GUN("연천군"),
    OSAN_SI("오산시"),
    UIWANG_SI("의왕시"),
    UIJEONGBU_SI("의정부시"),
    ICHEON_SI("이천시"),
    PAJU_SI("파주시"),
    PYEONGTAEK_SI("평택시"),
    POCHEON_SI("포천시"),
    HANAM_SI("하남시"),
    HWASEONG_SI("화성시"),
    ;

    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Sido getParentSido() {
        return Sido.GYEONGGI;
    }
}
