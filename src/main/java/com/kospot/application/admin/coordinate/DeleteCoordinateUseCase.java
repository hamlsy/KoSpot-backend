package com.kospot.application.admin.coordinate;

import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCoordinateUseCase {

    private final CoordinateAdaptor coordinateAdaptor;

    @Transactional
    public void execute(Member admin, Long coordinateId) {
        admin.validateAdmin();

        Coordinate coordinate = coordinateAdaptor.findById(coordinateId);
        coordinateAdaptor.delete(coordinate);
    }
}

