package com.kospot.application.coordinate;

import com.kospot.domain.coordinate.service.CoordinateExcelService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
