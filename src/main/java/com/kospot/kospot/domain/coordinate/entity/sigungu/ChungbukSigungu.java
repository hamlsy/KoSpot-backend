package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChungbukSigungu implements Sigungu {

    CHEONGJU_SI("청주시"),
    CHUNGJU_SI("충주시"),
    JECHEON_SI("제천시"),
    BOEUN_GUN("보은군"),
    OKCHEON_GUN("옥천군"),
    YEONGDONG_GUN("영동군"),
    JEUNGPYEONG_GUN("증평군"),
    JINCHEON_GUN("진천군"),
    GOESAN_GUN("괴산군"),
    EUMSEONG_GUN("음성군"),
    DANYANG_GUN("단양군"),
    ;
    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.CHUNGBUK;
    }

}