package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import com.kospot.kospot.domain.coordinateIdCache.repository.CoordinateIdCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService{

    private final CoordinateRepository coordinateRepository;
    private final CoordinateIdCacheRepository coordinateIdCacheRepository;
    private final DynamicCoordinateRepositoryFactory factory;

    /**
     * todo 예외처리 리팩터링
     * @param sidoName
     * @return
     */

    @Override
    public Location getRandomCoordinateBySido(String sidoName) {
        Sido sido = Sido.fromName(sidoName);
        Long maxId = getMaxId(sido);

        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);
        BaseCoordinateRepository<?, Long> repository = factory.getRepository(sido);

        do {
            if (repository.existsById(randomIndex)) {
                break;
            }
            randomIndex++;
        }while(true);

        return (Location) repository.findById(randomIndex).orElseThrow(
                () -> new IllegalArgumentException("해당 시도의 좌표가 존재하지 않습니다.")
        );
    }

    @Override
    public Coordinate getAllRandomCoordinate(){
        Long maxId = getMaxId(Sido.NATIONWIDE);
        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);

        do {
            if (coordinateRepository.existsById(randomIndex)) {
                break;
            }
            randomIndex++;
        }while(true);

        return coordinateRepository.findById(randomIndex).orElseThrow(
                () -> new IllegalArgumentException("해당 시도의 좌표가 존재하지 않습니다.")
        );

    }

    private Long getMaxId(Sido sido){
        return coordinateIdCacheRepository.findById(sido).orElseThrow(
                () -> new IllegalArgumentException("캐시 테이블의 ID 값이 존재하지 않습니다.")
        ).getMaxId();
    }

}
