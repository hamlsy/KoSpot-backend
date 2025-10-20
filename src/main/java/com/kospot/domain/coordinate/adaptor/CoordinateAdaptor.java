package com.kospot.domain.coordinate.adaptor;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.domain.coordinate.repository.nationwide.CoordinateNationwideRepository;
import com.kospot.infrastructure.exception.object.domain.CoordinateHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;

import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinateAdaptor {

    private final CoordinateRepository coordinateRepository;

    public Coordinate queryById(Long id) {
        return coordinateRepository.findById(id)
                .orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );
    }

    public Coordinate getRandomCoordinateBySido(Sido sido) {
        long count = coordinateRepository.countBySido(sido);
        if (count == 0) return null;

        long randomOffset = ThreadLocalRandom.current().nextLong(count);
        return coordinateRepository.findBySidoWithOffset(sido, randomOffset);
    }

    public Coordinate getRandomCoordinate() {
        long count = coordinateRepository.countAll();
        if (count == 0) return null;

        long randomOffset = ThreadLocalRandom.current().nextLong(count);
        return coordinateRepository.findByRandomOffset(randomOffset);
    }

}
