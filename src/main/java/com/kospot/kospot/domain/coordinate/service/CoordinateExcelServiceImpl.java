package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Address;
import com.kospot.kospot.domain.coordinate.entity.LocationType;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.sigungu.Sigungu;
import com.kospot.kospot.domain.coordinate.entity.sigungu.converter.SigunguConverter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinateExcelServiceImpl implements CoordinateExcelService{
    private final JdbcTemplate jdbcTemplate;

    private static final String FILE_PATH = "/data/excel/"; //todo refactor

    //excel row -> Coordinate
    private Coordinate rowToCoordinate(Row row) {
        Sido sido = Sido.fromName(getCellString(row, 0));
        Sigungu sigungu = SigunguConverter.convertSidoToSigungu(sido, getCellString(row, 1));
        String detailAddress = getCellString(row, 2);

        double lng = row.getCell(4).getNumericCellValue();
        double lat = row.getCell(5).getNumericCellValue();

        String poiName = getCellString(row,7); //H line
        String locationTypeString = getCellString(row,8); //I line
        LocationType locationType = LocationType.fromString(locationTypeString);

        Address address = Address.builder()
                .sido(sido)
                .sigungu(sigungu)
                .detailAddress(detailAddress)
                .build();
        return Coordinate.builder()
                .address(address)
                .poiName(poiName)
                .lng(lng)
                .lat(lat)
                .locationType(locationType)
                .build();
    }

    private String getCellString(Row row, int index){
        return row.getCell(index).getStringCellValue();
    }

}
