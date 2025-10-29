package com.kospot.application.admin.coordinate;

import com.kospot.domain.coordinate.service.CoordinateExcelService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ImportCoordinateByExcelUseCase {

    private final CoordinateExcelService coordinateExcelService;

    @Transactional
    public void execute(Member admin, String fileName) {
        admin.validateAdmin();
        coordinateExcelService.importCoordinatesFromExcel(fileName);
    }
}

