package com.kospot.kospot.domain.coordinate.entity.eupmyeondong;

import com.kospot.kospot.domain.coordinate.entity.sigungu.SejongSigungu;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SejongEupmyeondong implements Eupmyeondong{
    Garam_Dong("가람동"),
    Goun_Dong("고운동"),
    Geumnam_Myeon("금남면"),
    Naseong_Dong("나성동"),
    Dajeong_Dong("다정동"),
    Daepyeong_Dong("대평동"),
    Dodam_Dong("도담동"),
    Bangok_Dong("반곡동"),
    Boram_Dong("보람동"),
    Bugang_Myeon("부강면"),
    Saerom_Dong("새롬동"),
    Sodam_Dong("소담동"),
    Sojeong_Myeon("소정면"),
    Areum_Dong("아름동"),
    Eojin_Dong("어진동"),
    Yeongi_Myeon("연기면"),
    Yeondong_Myeon("연동면"),
    Yeonseo_Myeon("연서면"),
    Janggun_Myeon("장군면"),
    Jeondong_Myeon("전동면"),
    Jeonui_Myeon("전의면"),
    Jochiwon_Eup("조치원읍"),
    Jongchon_Dong("종촌동"),
    Hansol_Dong("한솔동");

    private final String name;

    @Override
    public SejongSigungu getParentSigungu() {
        return SejongSigungu.SEJONG_SI;
    }
}
