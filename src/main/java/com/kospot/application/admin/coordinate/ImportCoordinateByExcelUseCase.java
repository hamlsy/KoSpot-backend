package com.kospot.application.admin.coordinate;

import com.kospot.domain.coordinate.service.CoordinateExcelService;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@UseCase
@RequiredArgsConstructor
public class ImportCoordinateByExcelUseCase {

    private final MemberAdaptor memberAdaptor;
    private final CoordinateExcelService coordinateExcelService;

    @Transactional
    public void execute(Long adminId, MultipartFile file) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();
        coordinateExcelService.importCoordinatesFromExcelFile(file);
    }
}

