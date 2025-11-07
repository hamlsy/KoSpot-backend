package com.kospot.application.admin.coordinate;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.LocationType;
import com.kospot.domain.coordinate.entity.Sido;
import com.kospot.domain.coordinate.service.CoordinateService;
import com.kospot.domain.coordinate.vo.Address;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.request.AdminCoordinateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateCoordinateUseCase {

    private final CoordinateService coordinateService;

    @Transactional
    public Long execute(Member admin, AdminCoordinateRequest.Create request) {
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

