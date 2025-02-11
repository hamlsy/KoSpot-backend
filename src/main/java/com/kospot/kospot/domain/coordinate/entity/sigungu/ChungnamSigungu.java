package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChungnamSigungu implements Sigungu {

    CHEONAN_SI("천안시"),
    GONGJU_SI("공주시"),
    BORYEONG_SI("보령시"),
    ASAN_SI("아산시"),
    SEOSAN_SI("서산시"),
    NONSAN_SI("논산시"),
    GYERYONG_SI("계룡시"),
    DANGJIN_SI("당진시"),
    GEUMSAN_GUN("금산군"),
    BUYEOGUN("부여군"),
    SEOCHEON_GUN("서천군"),
    CHEONGYANG_GUN("청양군"),
    HONGSEONG_GUN("홍성군"),
    YESAN_GUN("예산군"),
    TAEAN_GUN("태안군"),
    ;

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.CHUNGNAM;
    }

}
