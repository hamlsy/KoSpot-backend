package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Address;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.sigungu.Sigungu;
import com.kospot.kospot.domain.coordinate.entity.sigungu.converter.SigunguConverter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinateExcelServiceImpl implements CoordinateExcelService{

    //excel row -> Coordinate
    private Coordinate rowToCoordinate(Row row) {
        Sido sido = Sido.fromName(getCellString(row, 0));
        Sigungu sigungu = SigunguConverter.convertSidoToSigungu(sido, getCellString(row, 1));
        String detailAddress = getCellString(row, 2);
        double lng = row.getCell(4).getNumericCellValue();
        double lat = row.getCell(5).getNumericCellValue();

        Address address = Address.builder()
                .sido(sido)
                .sigungu(sigungu)
                .detailAddress(detailAddress)
                .build();
        return Coordinate.builder()
                .address(address)
                .lng(lng)
                .lat(lat)
                .build();
    }

    private String getCellString(Row row, int index){
        return row.getCell(index).getStringCellValue();
    }

}
