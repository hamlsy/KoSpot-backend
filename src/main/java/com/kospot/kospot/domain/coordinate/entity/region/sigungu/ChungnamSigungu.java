package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChungnamSigungu implements Sigungu {

    GOESAN_GUN("괴산군"),
    DANYANG_GUN("단양군"),
    BOEUN_GUN("보은군"),
    YEONGDONG_GUN("영동군"),
    OKCHEON_GUN("옥천군"),
    EUMSEONG_GUN("음성군"),
    JECHEON_SI("제천시"),
    JEUNGPYEONG_GUN("증평군"),
    JINCHEON_GUN("진천군"),
    CHEONGJU_SI("청주시"),
    CHUNGJU_SI("충주시"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.CHUNGNAM;
    }

}
