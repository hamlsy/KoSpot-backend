package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusanSigungu implements Sigungu {

    GANGSEO_GU("강서구"),
    GEUMJEONG_GU("금정구"),
    GIJANG_GUN("기장군"),
    NAM_GU("남구"),
    DONG_GU("동구"),
    DONGNAE_GU("동래구"),
    BUSANJIN_GU("부산진구"),
    BUK_GU("북구"),
    SASANG_GU("사상구"),
    SAHA_GU("사하구"),
    SEO_GU("서구"),
    SUYEONG_GU("수영구"),
    YEONJE_GU("연제구"),
    YEONGDO_GU("영도구"),
    JUNG_GU("중구"),
    HAEUNDAE_GU("해운대구");

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.BUSAN;
    }

}
