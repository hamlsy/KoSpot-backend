package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GangwonSigungu implements Sigungu{

    GANGNEUNG_SI("강릉시"),
    GOSEONG_GUN("고성군"),
    DONGHAE_SI("동해시"),
    SAMCHEOK_SI("삼척시"),
    SOKCHO_SI("속초시"),
    YANGGU_GUN("양구군"),
    YANGYANG_GUN("양양군"),
    YEONGWOL_GUN("영월군"),
    WONJU_SI("원주시"),
    INJE_GUN("인제군"),
    JEONGSEON_GUN("정선군"),
    CHEORWON_GUN("철원군"),
    CHUNCHEON_SI("춘천시"),
    TAEBAEK_SI("태백시"),
    PYEONGCHANG_GUN("평창군"),
    HONGCHEON_GUN("홍천군"),
    HWACHEON_GUN("화천군"),
    HOENGSEONG_GUN("횡성군"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.GANGWON;
    }
}
