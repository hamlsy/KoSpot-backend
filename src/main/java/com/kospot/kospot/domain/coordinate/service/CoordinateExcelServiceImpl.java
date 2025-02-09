package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Address;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.LocationType;
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

                Coordinate coordinate = rowToCoordinate(row);
                Sido sido = coordinate.getAddress().getSido();

                coordinatesMap.putIfAbsent(sido, new ArrayList<>());
                coordinatesMap.get(sido).add(coordinate);

                coordinatesMap.putIfAbsent(Sido.NATIONWIDE, new ArrayList<>());
                coordinatesMap.get(Sido.NATIONWIDE).add(coordinate);

                // BATCH_SIZE마다 저장
                if (coordinatesMap.get(sido).size() >= BATCH_SIZE) {
                    saveCoordinates(sido, coordinatesMap.get(sido));
                    coordinatesMap.get(sido).clear(); // 리스트 초기화
                }
            }

            // 나머지 저장
            for(Sido sido : coordinatesMap.keySet()) {
                saveCoordinates(sido, coordinatesMap.get(sido));
            }

        } catch (FileNotFoundException e) {
            throw new CoordinateHandler(ErrorStatus.FILE_NOT_FOUND);
        } catch (IOException e) {
            throw new CoordinateHandler(ErrorStatus.FILE_READ_ERROR);
        }

    }

    private void saveCoordinates(Sido sido, List<Coordinate> coordinates) {
        repositoryFactory.getRepository(sido).saveAll(coordinates);
        System.out.println(repositoryFactory.getRepository(sido).getClass().getName() + " saved " + coordinates.size() + " coordinates");
    }

    //excel row -> Coordinate
    private CoordinateNationwide rowToCoordinate(Row row) {
        Sido sido = Sido.fromName(getCellString(row, 0));
        Sigungu sigungu = SigunguConverter.convertSidoToSigungu(sido, getCellString(row, 1));
        String detailAddress = getCellString(row, 2);

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
