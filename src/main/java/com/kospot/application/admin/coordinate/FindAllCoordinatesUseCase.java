package com.kospot.application.admin.coordinate;

import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminCoordinateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UseCase
@RequiredArgsConstructor
public class FindAllCoordinatesUseCase {

    private final CoordinateAdaptor coordinateAdaptor;

    public Page<AdminCoordinateResponse.CoordinateInfo> execute(Member admin, Pageable pageable) {
        admin.validateAdmin();

        return coordinateAdaptor.queryAll(pageable)
                .map(AdminCoordinateResponse.CoordinateInfo::from);
    }
}

