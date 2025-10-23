package com.kospot.domain.coordinate.adaptor;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinateAdaptor {

    private final CoordinateRepository coordinateRepository;

    // 특정 Sido의 랜덤 Coordinate
    public Coordinate getRandomCoordinateBySido(Sido sido) {
        long count = coordinateRepository.countBySido(sido);
        if (count == 0) return null;

        long randomOffset = ThreadLocalRandom.current().nextLong(count);
        return coordinateRepository.findBySidoWithOffset(sido,
                PageRequest.of((int)(randomOffset / 1), 1)).getContent().get(0);
    }

    // 전체 랜덤 Coordinate
    // todo refactoring
    public Coordinate getRandomCoordinate() {
        long count = coordinateRepository.countAllNative();
        if (count == 0) return null;
        int randomOffset = ThreadLocalRandom.current().nextInt((int) count);

        return coordinateRepository.findAllCoordinates(PageRequest.of(randomOffset, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

}
