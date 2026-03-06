package com.kospot.application.admin.coordinate;

import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.domain.entity.LocationType;
import com.kospot.coordinate.domain.entity.Sido;
import com.kospot.coordinate.application.service.CoordinateService;
import com.kospot.coordinate.domain.vo.Address;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.request.AdminCoordinateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCoordinateUseCase {

    private final MemberAdaptor memberAdaptor;
    private final CoordinateService coordinateService;

    @Transactional
    public Long execute(Long adminId, AdminCoordinateRequest.Create request) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        Sido sido = Sido.fromKey(request.getSidoKey());
        String fullAddress = sido.getName() + " " + request.getSigungu() + " " + request.getDetailAddress();

        Address address = Address.builder()
                .sido(sido)
                .sigungu(request.getSigungu())
                .detailAddress(request.getDetailAddress())
                .fullAddress(fullAddress)
                .build();

        Coordinate coordinate = Coordinate.builder()
                .lat(request.getLat())
                .lng(request.getLng())
                .poiName(request.getPoiName())
                .address(address)
                .locationType(LocationType.fromString(request.getLocationType()))
                .build();

        Coordinate saved = coordinateService.createCoordinate(coordinate);
        return saved.getId();
    }
}

