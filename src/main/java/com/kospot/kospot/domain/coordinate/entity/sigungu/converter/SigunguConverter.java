package com.kospot.kospot.domain.coordinate.entity.sigungu.converter;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.sigungu.*;
import jakarta.persistence.AttributeConverter;

public class SigunguConverter implements AttributeConverter<Sigungu, String> {

    @Override
    public String convertToDatabaseColumn(Sigungu attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getName();
    }

    @Override
    public Sigungu convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // Assuming SEOUL as default Sido for simplicity
        return convertSidoToSigungu(Sido.SEOUL, dbData);
    }

    public static Sigungu convertSidoToSigungu(Sido sido, String sigunguName) {
        switch (sido) {
            case SEOUL:
                return Sigungu.fromName(SeoulSigungu.class, sigunguName);
            case GYEONGGI:
                return Sigungu.fromName(GyeonggiSigungu.class, sigunguName);
            case INCHEON:
                return Sigungu.fromName(IncheonSigungu.class, sigunguName);
            case BUSAN:
                return Sigungu.fromName(BusanSigungu.class, sigunguName);
            case DAEGU:
                return Sigungu.fromName(DaeguSigungu.class, sigunguName);
            case GWANGJU:
                return Sigungu.fromName(GwangjuSigungu.class, sigunguName);
            case DAEJEON:
                return Sigungu.fromName(DaejeonSigungu.class, sigunguName);
            case ULSAN:
                return Sigungu.fromName(UlsanSigungu.class, sigunguName);
            case SEJONG:
                return Sigungu.fromName(SejongSigungu.class, sigunguName);
            case GANGWON:
                return Sigungu.fromName(GangwonSigungu.class, sigunguName);
            case CHUNGBUK:
                return Sigungu.fromName(ChungbukSigungu.class, sigunguName);
            case CHUNGNAM:
                return Sigungu.fromName(ChungnamSigungu.class, sigunguName);
            case JEONBUK:
                return Sigungu.fromName(JeonbukSigungu.class, sigunguName);
            case JEONNAM:
                return Sigungu.fromName(JeonnamSigungu.class, sigunguName);
            case GYEONGBUK:
                return Sigungu.fromName(GyeongbukSigungu.class, sigunguName);
            case GYEONGNAM:
                return Sigungu.fromName(GyeongnamSigungu.class, sigunguName);
            case JEJU:
                return Sigungu.fromName(JejuSigungu.class, sigunguName);
            default:
                //todo : exception handling
                throw new IllegalArgumentException();
        }
    }

}
