package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.repository.CoordinateNationwideRepository;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinateAdaptorImpl implements CoordinateAdaptor{

    private final CoordinateNationwideRepository repository;

    @Override
    public Coordinate queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );
    }

    @Override
    public boolean queryExistsById(Long id) {
        return repository.existsById(id);
    }
}
