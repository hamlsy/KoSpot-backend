package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChungbukSigungu implements Sigungu {

    GYERYONG_SI("계룡시"),
    GONGJU_SI("공주시"),
    GEUMSAN_GUN("금산군"),
    NONSAN_SI("논산시"),
    DANGJIN_SI("당진시"),
    BORYEONG_SI("보령시"),
    BUYEOGUN("부여군"),
    SEOSAN_SI("서산시"),
    SEOCHEON_GUN("서천군"),
    ASAN_SI("아산시"),
    YESAN_GUN("예산군"),
    CHEONAN_SI("천안시"),
    CHEONGYANG_GUN("청양군"),
    TAEAN_GUN("태안군"),
    HONGSEONG_GUN("홍성군"),
    ;
    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.CHUNGBUK;
    }

}