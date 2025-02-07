package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoordinateAdaptorImpl implements CoordinateAdaptor{

    private final CoordinateRepository repository;

    @Override
    public Location queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );
    }

    @Override
    public boolean queryExistsById(Long id) {
        return repository.existsById(id);
    }
}
