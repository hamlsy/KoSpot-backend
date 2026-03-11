package com.kospot.admin.application.usecase.coordinate;

import com.kospot.coordinate.application.service.CoordinateExcelService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
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

