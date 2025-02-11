package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JeonnamSigungu implements Sigungu {

    MOKPO_SI("목포시"),
    YEOSU_SI("여수시"),
    SUNCHEON_SI("순천시"),
    NAJU_SI("나주시"),
    GWANGYANG_SI("광양시"),
    DAMYANG_GUN("담양군"),
    GOKSEONG_GUN("곡성군"),
    GURYE_GUN("구례군"),
    GOHEUNG_GUN("고흥군"),
    BOSEONG_GUN("보성군"),
    HWASUN_GUN("화순군"),
    JANGHEUNG_GUN("장흥군"),
    GANGJIN_GUN("강진군"),
    HAENAM_GUN("해남군"),
    YEONGAM_GUN("영암군"),
    MUAN_GUN("무안군"),
    HAMPYEONG_GUN("함평군"),
    YEONGGWANG_GUN("영광군"),
    JANGSEONG_GUN("장성군"),
    WANDO_GUN("완도군"),
    JINDO_GUN("진도군"),
    SINAN_GUN("신안군"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.JEONNAM;
    }

}
