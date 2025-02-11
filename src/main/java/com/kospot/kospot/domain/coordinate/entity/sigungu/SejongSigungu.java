package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SejongSigungu implements Sigungu{
    SEJONG_SI("세종특별자치시");

    private final String name;


    @Override
    public Sido getParentSido() {
        return Sido.SEJONG;
    }
}
