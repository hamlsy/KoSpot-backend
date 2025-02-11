package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.kospot.domain.coordinate.service.DynamicCoordinateRepositoryFactory;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinateAdaptorImpl implements CoordinateAdaptor{

    private final CoordinateRepository repository;
    private final DynamicCoordinateRepositoryFactory factory;

    @Override
    public Coordinate queryById(Sido sido, Long id) {
        return (Coordinate) factory.getRepository(sido).findById(id).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );
    }

    @Override
    public boolean queryExistsById(Sido sido, Long id) {
        return factory.getRepository(sido).existsById(id);
    }

    public Long queryMaxIdBySido(Sido sido){
        return null;
    }
}
