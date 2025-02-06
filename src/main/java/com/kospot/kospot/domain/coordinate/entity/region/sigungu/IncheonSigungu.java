package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IncheonSigungu implements Sigungu{
    GANGHWA_GUN("강화군"),
    GYEYANG_GU("계양구"),
    NAMDONG_GU("남동구"),
    DONG_GU("동구"),
    MICHUHOL_GU("미추홀구"),
    BUPYEONG_GU("부평구"),
    SEO_GU("서구"),
    YEONSU_GU("연수구"),
    ONGJIN_GUN("옹진군"),
    JUNG_GU("중구");

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.INCHEON;
    }
}
