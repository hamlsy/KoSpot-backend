package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Address;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.LocationType;
import com.kospot.kospot.domain.coordinate.entity.converter.CoordinateConverter;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.sigungu.Sigungu;
import com.kospot.kospot.domain.coordinate.entity.sigungu.converter.SigunguConverter;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CoordinateExcelServiceImpl implements CoordinateExcelService {

    private final DynamicCoordinateRepositoryFactory repositoryFactory;
    private static final String FILE_PATH = "data/excel/";

    private final int BATCH_SIZE = 1000;
    private final Sido NATIONWIDE = Sido.NATIONWIDE;

    @Override
    @Transactional
    public void importCoordinatesFromExcel(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(FILE_PATH + fileName);
            FileInputStream fis = new FileInputStream(resource.getFile());

            //excel -> Apache POI workbook 객체로 로드
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트
            Iterator<Row> rowIterator = sheet.iterator(); // 반복 객체 생성

            // 지역별 좌표 리스트를 저장하는 Map
            Map<Sido, List<Coordinate>> coordinatesMap = new HashMap<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0) continue; // 첫 번째 줄은 헤더이므로 건너뜀

                CoordinateNationwide coordinateNationwide = rowToCoordinateNationwide(row);
                Coordinate coordinate = CoordinateConverter.convertToDetailCoordinate(coordinateNationwide);
                Sido sido = coordinate.getAddress().getSido();

                // detail coordinate
                coordinatesMap.putIfAbsent(sido, new ArrayList<>());
                coordinatesMap.get(sido).add(coordinate);

                // nation wide coordinate
                coordinatesMap.putIfAbsent(NATIONWIDE, new ArrayList<>());
                coordinatesMap.get(NATIONWIDE).add(coordinateNationwide);

                // BATCH_SIZE 마다 저장
                // todo refactoring
                if (isBatchSizeReached(sido, coordinatesMap)) {
                    saveCoordinates(sido, coordinatesMap.get(sido));
                    coordinatesMap.get(sido).clear();
                }

                if (isBatchSizeReached(NATIONWIDE, coordinatesMap)) {
                    saveCoordinates(NATIONWIDE, coordinatesMap.get(NATIONWIDE));
                    coordinatesMap.get(NATIONWIDE).clear();
                }

            }

            // 나머지 저장
            for (Sido sido : coordinatesMap.keySet()) {
                saveCoordinates(sido, coordinatesMap.get(sido));
            }

        } catch (FileNotFoundException e) {
            throw new CoordinateHandler(ErrorStatus.FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new CoordinateHandler(ErrorStatus.FILE_READ_ERROR);
        }

    }

    private boolean isBatchSizeReached(Sido sido, Map<Sido, List<Coordinate>> coordinatesMap) {
        return coordinatesMap.get(sido).size() >= BATCH_SIZE;
    }

    private void saveCoordinates(Sido sido, List<Coordinate> coordinates) {
        repositoryFactory.getRepository(sido).saveAll(coordinates);
    }

    //excel row -> CoordinateNationwide
    private CoordinateNationwide rowToCoordinateNationwide(Row row) {
        Sido sido = Sido.fromName(getCellString(row, 0)); //A
        Sigungu sigungu = SigunguConverter.convertSidoToSigungu(sido, getCellString(row, 1)); //B
        String cLine = getCellString(row, 2);
        String dLine = getCellString(row, 3);
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

        return CoordinateNationwide.builder()
                .address(address)
                .poiName(poiName)
                .lng(lng)
                .lat(lat)
                .locationType(locationType)
                .build();
    }

    private String getCellString(Row row, int index) {
        return row.getCell(index).getStringCellValue();
    }

}
