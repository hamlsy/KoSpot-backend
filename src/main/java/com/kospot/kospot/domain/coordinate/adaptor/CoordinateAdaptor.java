package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.CoordinateNationwideRepository;
import com.kospot.kospot.domain.coordinate.service.DynamicCoordinateRepositoryFactory;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinateAdaptor {

    private final DynamicCoordinateRepositoryFactory factory;
    private final CoordinateNationwideRepository nationwideRepository;

    public Coordinate queryById(Sido sido, Long id) {
        return factory.getRepository(sido).findById(id)
                .map(Coordinate.class::cast)
                .orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );
    }

    public boolean queryExistsById(Sido sido, Long id) {
        return factory.getRepository(sido).existsById(id);
    }

    public Long queryMaxIdBySido(Sido sido){
        Long maxId = factory.getRepository(sido).findMaxId();
        return Optional.ofNullable(maxId).orElse(0L);
    }

    /**
     *  Nationwide
     */

    public Coordinate queryNationwideById(Long id) {
        return nationwideRepository.findById(id)
                .map(Coordinate.class::cast)
                .orElseThrow(
                        () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
                );
    }

    public boolean queryNationwideExistsById(Long id) {
        return nationwideRepository.existsById(id);
    }

    public Long queryNationwideMaxId() {
        Long maxId = nationwideRepository.findMaxId();
        return Optional.ofNullable(maxId).orElse(0L);
    }
}
