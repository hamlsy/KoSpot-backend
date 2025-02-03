package com.kospot.kospot.domain.coordinate.entity.region.sigungu;

import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SeoulSigungu implements Sigungu{
    GANGBUK("강북구"),
    GANGDONG("강동구"),
    GANGNAM("강남구"),
    GANGSEO("강서구"),
    GEUMCHEON("금천구"),
    GURO("구로구"),
    GWANAK("관악구"),
    GWANGJIN("광진구"),
    JONGNO("종로구"),
    JUNG("중구"),
    JUNGRANG("중랑구"),
    MAPO("마포구"),
    NOWON("노원구"),
    DOBONG("도봉구"),
    DONGDAEMUN("동대문구"),
    DONGJAK("동작구"),
    SEODAEMUN("서대문구"),
    SEOCHO("서초구"),
    SEONGDONG("성동구"),
    SEONGBUK("성북구"),
    SONGPA("송파구"),
    YANGCHEON("양천구"),
    YEONGDEUNGPO("영등포구"),
    YONGSAN("용산구"),
    EUNPYEONG("은평구"),
    ;

    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Sido getParentSido() {
        return Sido.SEOUL;
    }
}
