package com.kospot.admin.application.usecase.coordinate;

import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.admin.presentation.dto.response.AdminCoordinateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@UseCase
@RequiredArgsConstructor
public class FindAllCoordinatesUseCase {

    private final MemberAdaptor memberAdaptor;
    private final CoordinateAdaptor coordinateAdaptor;

    public Page<AdminCoordinateResponse.CoordinateInfo> execute(Long adminId, Pageable pageable) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        return coordinateAdaptor.queryAll(pageable)
                .map(AdminCoordinateResponse.CoordinateInfo::from);
    }
}

