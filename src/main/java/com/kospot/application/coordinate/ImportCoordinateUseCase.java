package com.kospot.application.coordinate;

import com.kospot.domain.coordinate.service.CoordinateExcelService;
import com.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ImportCoordinateUseCase {

    private final CoordinateExcelService coordinateExcelService;

    public void execute(String fileName) {
        coordinateExcelService.importCoordinatesFromExcel(fileName);
    }

}
