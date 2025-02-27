package com.kospot.kospot.application.coordinate;

import com.kospot.kospot.domain.coordinate.service.CoordinateExcelService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ImportCoordinateUseCase {

    private final CoordinateExcelService coordinateExcelService;

    public void execute(String fileName) {
        coordinateExcelService.importCoordinatesFromExcel(fileName);
    }

}
