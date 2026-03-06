package com.kospot.coordinate.application.usecase;

import com.kospot.coordinate.application.service.CoordinateExcelService;
import com.kospot.common.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@UseCase
@RequiredArgsConstructor
public class ImportCoordinateUseCase {

    private final CoordinateExcelService coordinateExcelService;

    public void execute(MultipartFile file) {
        coordinateExcelService.importCoordinatesFromExcelFile(file);
    }

}
