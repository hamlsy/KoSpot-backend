package com.kospot.kospot.domain.coordinate.entity;

import lombok.Getter;

public enum LocationType {
    TOURIST_ATTRACTION,  // 관광지 (유명 관광지, 궁궐, 문화유산, 성, 왕릉 등)
    NATURE_LEISURE,      // 자연 및 레저 (폭포, 계곡, 해수욕장, 캠핑, 동물원 등)
    URBAN_FACILITY,      // 도시 시설 (공항, 관광안내소, 테마파크, 영어마을 등)
    FESTIVAL_CULTURE, // 축제 및 문화 (지역축제, 영화촬영지, 전통마을, 온천 등)
    NONE
}
