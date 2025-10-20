package com.kospot.domain.coordinate.service;

import com.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.domain.coordinate.vo.Address;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.LocationType;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.infrastructure.exception.object.domain.CoordinateHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CoordinateExcelService {

    private static final String FILE_PATH = "data/excel/";
    private final int BATCH_SIZE = 1000;
    private final CoordinateRepository coordinateRepository;

    @Transactional
    public void importCoordinatesFromExcel(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(FILE_PATH + fileName);
            FileInputStream fis = new FileInputStream(resource.getFile());

            //excel -> Apache POI workbook 객체로 로드
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트
            Iterator<Row> rowIterator = sheet.iterator(); // 반복 객체 생성

            // 좌표 리스트를 저장하는 List
            List<Coordinate> coordinatesList = new ArrayList<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0 || isRowEmpty(row)) continue; // 첫 번째 줄은 헤더이므로 건너뜀

                Coordinate coordinate = rowToCoordinate(row);

                // coordinate
                coordinatesList.add(coordinate);

                // BATCH_SIZE 마다 저장
                // todo refactoring
                if (isBatchSizeReached(coordinatesList)) {
                    saveCoordinates(coordinatesList);
                    coordinatesList.clear();
                }
            }

            // 나머지 저장
            saveCoordinates(coordinatesList);
            coordinatesList.clear();


        } catch (FileNotFoundException e) {
            throw new CoordinateHandler(ErrorStatus.FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new CoordinateHandler(ErrorStatus.FILE_READ_ERROR);
        }

    }
    private boolean isRowEmpty(Row row) {
        if (row == null) return true; //row 자체가 null이면 빈 행으로 처리

        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false; //
            }
        }
        return true; //
    }


    private boolean isBatchSizeReached(List<Coordinate> coordinatesList) {
        return coordinatesList.size() >= BATCH_SIZE;
    }

    private void saveCoordinates(List<Coordinate> coordinates) {
        coordinateRepository.saveAll(coordinates);
    }

    //excel row -> CoordinateNationwide
    private Coordinate rowToCoordinate(Row row) {
        Sido sido = Sido.fromName(getCellString(row, 0)); //A
        String sigungu = getCellString(row, 1); //B
        String cLine = getCellString(row, 2); //C
        String dLine = getCellString(row, 3);//D
        String detailAddress = cLine + " " + dLine;

        double lng = row.getCell(4).getNumericCellValue();
        double lat = row.getCell(5).getNumericCellValue();

        String poiName = getCellString(row, 7); //H line
        String locationTypeString = getCellString(row, 8); //I line
        LocationType locationType = LocationType.fromString(locationTypeString);

        Address address = Address.builder()
                .sido(sido)
                .sigungu(sigungu)
                .detailAddress(detailAddress)
                .build();

        return Coordinate.builder()
                .address(address)
                .poiName(poiName)
                .sido(sido)
                .lng(lng)
                .lat(lat)
                .locationType(locationType)
                .build();
    }

    private String getCellString(Row row, int index) {
        return row.getCell(index).getStringCellValue();
    }

}
