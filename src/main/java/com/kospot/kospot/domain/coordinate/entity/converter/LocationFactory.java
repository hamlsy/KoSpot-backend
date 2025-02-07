package com.kospot.kospot.domain.coordinate.entity.converter;

import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.*;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;

public class LocationFactory {
    public static Location createLocationBySido(Sido sido){
        return switch (sido) {
            case SEOUL -> new CoordinateSeoul();
            case BUSAN -> new CoordinateBusan();
            case DAEGU -> new CoordinateDaegu();
            case INCHEON -> new CoordinateIncheon();
            case GWANGJU -> new CoordinateGwangju();
            case DAEJEON -> new CoordinateDaejeon();
            case ULSAN -> new CoordinateUlsan();
            case SEJONG -> new CoordinateSejong();
            case GYEONGGI -> new CoordinateGyeonggi();
            case GANGWON -> new CoordinateGangwon();
            case CHUNGBUK -> new CoordinateChungbuk();
            case CHUNGNAM -> new CoordinateChungnam();
            case JEONBUK -> new CoordinateJeonbuk();
            case JEONNAM -> new CoordinateJeonnam();
            case GYEONGBUK -> new CoordinateGyeongbuk();
            case GYEONGNAM -> new CoordinateGyeongnam();
            case JEJU -> new CoordinateJeju();
            default -> throw new CoordinateHandler(ErrorStatus.SIDO_NOT_FOUND);
        };
    }
}