package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.kospot.domain.coordinate.entity.Address;
import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.entity.sigungu.Sigungu;
import com.kospot.kospot.domain.coordinate.entity.sigungu.converter.SigunguConverter;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinateIdCache.adaptor.CoordinateIdCacheAdaptor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService {

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateIdCacheAdaptor coordinateIdCacheAdaptor;
    private final DynamicCoordinateRepositoryFactory factory;
    private final JdbcTemplate jdbcTemplate;

    private static final String FILE_PATH = "/data/excel/"; //todo refactor

    @Override
    public Location getRandomCoordinateBySido(String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Long maxId = getMaxId(sido);
        Long randomIndex = getRandomIndex(maxId);

        BaseCoordinateRepository<?, Long> repository = factory.getRepository(sido);

        while (!repository.existsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(randomIndex);
    }

    @Override
    public Location getAllRandomCoordinate() {
        Long maxId = getMaxId(Sido.NATIONWIDE);
        Long randomIndex = getRandomIndex(maxId);

        while (!coordinateAdaptor.queryExistsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(randomIndex);
    }

    private Long getMaxId(Sido sido) {
        return coordinateIdCacheAdaptor.queryById(sido).getMaxId();
    }

    private Long getRandomIndex(Long maxId) {
        return ThreadLocalRandom.current().nextLong(maxId);
    }

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
