package com.kospot.domain.coordinate.entity;

import java.util.Map;

public enum LocationType {
    TOURIST_ATTRACTION,  // 관광지 (유명 관광지, 궁궐, 문화유산, 성, 왕릉 등)
    NATURE_LEISURE,      // 자연 및 레저 (폭포, 계곡, 해수욕장, 캠핑, 동물원 등)
    URBAN_FACILITY,      // 도시 시설 (공항, 관광안내소, 테마파크, 영어마을 등)
    FESTIVAL_CULTURE, // 축제 및 문화 (지역축제, 영화촬영지, 전통마을, 온천 등)
    NONE;

    private static final Map<String, LocationType> TYPE_MAP = Map.ofEntries(
            Map.entry("항공사/여행사", URBAN_FACILITY),
            Map.entry("관광안내소/매표소", URBAN_FACILITY),
            Map.entry("영어마을", URBAN_FACILITY),
            Map.entry("일반관광지", TOURIST_ATTRACTION),
            Map.entry("유명관광지", TOURIST_ATTRACTION),
            Map.entry("궁궐/종묘", TOURIST_ATTRACTION),
            Map.entry("비/탑/문/각", TOURIST_ATTRACTION),
            Map.entry("유명사적/유적지", TOURIST_ATTRACTION),
            Map.entry("보물", TOURIST_ATTRACTION),
            Map.entry("고택/생가/민속마을", TOURIST_ATTRACTION),
            Map.entry("서원/향교/서당", TOURIST_ATTRACTION),
            Map.entry("드라마/영화촬영지", TOURIST_ATTRACTION),
            Map.entry("국보", TOURIST_ATTRACTION),
            Map.entry("천연기념물", TOURIST_ATTRACTION),
            Map.entry("왕릉/고분", TOURIST_ATTRACTION),
            Map.entry("성/성터", TOURIST_ATTRACTION),
            Map.entry("지역축제", FESTIVAL_CULTURE),
            Map.entry("폭포/계곡", NATURE_LEISURE),
            Map.entry("일반유원지/일반놀이공원", NATURE_LEISURE),
            Map.entry("테마공원/대형놀이공원", NATURE_LEISURE),
            Map.entry("휴양림/수목원", NATURE_LEISURE),
            Map.entry("팜스테이", NATURE_LEISURE),
            Map.entry("캠핑장", NATURE_LEISURE),
            Map.entry("식물원", NATURE_LEISURE),
            Map.entry("관광농원/허브마을", NATURE_LEISURE),
            Map.entry("동물원", NATURE_LEISURE),
            Map.entry("야영장", NATURE_LEISURE),
            Map.entry("글램핑코리아(캠핑)", NATURE_LEISURE),
            Map.entry("캠핑홀리데이(캠핑)", NATURE_LEISURE),
            Map.entry("잼핑홀리데이(캠핑)", NATURE_LEISURE),
            Map.entry("정보화마을", NATURE_LEISURE),
            Map.entry("아쿠아리움/대형수족관", NATURE_LEISURE),
            Map.entry("해수욕장", NATURE_LEISURE),
            Map.entry("온천지역", NATURE_LEISURE),
            Map.entry("N", NONE)
    );

    public static LocationType fromString(String type) {
        return TYPE_MAP.getOrDefault(type, NONE);
    }
}
