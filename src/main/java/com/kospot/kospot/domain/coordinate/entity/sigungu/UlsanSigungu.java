package com.kospot.kospot.domain.coordinate.entity.sigungu;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UlsanSigungu implements Sigungu{
    BUK_GU("북구"),
    DONG_GU("동구"),
    JUNG_GU("중구"),
    NAM_GU("남구"),
    ULJU_GU("울주군");

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.ULSAN;
    }
}
