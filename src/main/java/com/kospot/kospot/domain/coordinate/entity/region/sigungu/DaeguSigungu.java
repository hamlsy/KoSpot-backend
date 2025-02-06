package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DaeguSigungu implements Sigungu{

    BUK_GU("북구"),
    DALSEO_GU("달서구"),
    DALSEONG_GUN("달성군"),
    DONG_GU("동구"),
    NAM_GU("남구"),
    SEO_GU("서구"),
    SUK_GU("수성구");

    private final String name;

    @Override
    public Sido getParentSido() {
        return Sido.DAEGU;
    }

}
