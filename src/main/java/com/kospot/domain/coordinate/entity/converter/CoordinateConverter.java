package com.kospot.domain.coordinate.entity.converter;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.coordinates.*;
import com.kospot.kospot.domain.coordinate.entity.coordinates.*;
import com.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.exception.payload.code.ErrorStatus;

public class CoordinateConverter {
    public static Coordinate convertToDetailCoordinate(CoordinateNationwide coordinate){
        return switch (coordinate.getAddress().getSido()){
            case SEOUL -> new CoordinateSeoul(coordinate);
            case BUSAN -> new CoordinateBusan(coordinate);
            case DAEGU -> new CoordinateDaegu(coordinate);
            case INCHEON -> new CoordinateIncheon(coordinate);
            case GWANGJU -> new CoordinateGwangju(coordinate);
            case DAEJEON -> new CoordinateDaejeon(coordinate);
            case ULSAN -> new CoordinateUlsan(coordinate);
            case SEJONG -> new CoordinateSejong(coordinate);
            case GYEONGGI -> new CoordinateGyeonggi(coordinate);
            case GANGWON -> new CoordinateGangwon(coordinate);
            case CHUNGBUK -> new CoordinateChungbuk(coordinate);
            case CHUNGNAM -> new CoordinateChungnam(coordinate);
            case JEONBUK -> new CoordinateJeonbuk(coordinate);
            case JEONNAM -> new CoordinateJeonnam(coordinate);
            case GYEONGBUK -> new CoordinateGyeongbuk(coordinate);
            case GYEONGNAM -> new CoordinateGyeongnam(coordinate);
            case JEJU -> new CoordinateJeju(coordinate);
            default -> throw new CoordinateHandler(ErrorStatus.SIDO_NOT_FOUND);
        };
    }
}
