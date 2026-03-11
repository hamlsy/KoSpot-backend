package com.kospot.admin.application.usecase.coordinate;

import com.kospot.coordinate.application.adaptor.CoordinateAdaptor;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.application.service.CoordinateService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteCoordinateUseCase {

    private final MemberAdaptor memberAdaptor;
    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateService coordinateService;

    @Transactional
    public void execute(Long adminId, Long coordinateId) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        Coordinate coordinate = coordinateAdaptor.queryById(coordinateId);
        coordinateService.deleteCoordinate(coordinate);
    }
}

