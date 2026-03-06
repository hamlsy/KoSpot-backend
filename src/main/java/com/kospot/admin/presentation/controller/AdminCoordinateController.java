package com.kospot.admin.presentation.controller;

import com.kospot.admin.application.usecase.coordinate.CreateCoordinateUseCase;
import com.kospot.admin.application.usecase.coordinate.DeleteCoordinateUseCase;
import com.kospot.admin.application.usecase.coordinate.FindAllCoordinatesUseCase;
import com.kospot.admin.application.usecase.coordinate.ImportCoordinateByExcelUseCase;
import com.kospot.common.exception.payload.code.SuccessStatus;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.admin.presentation.dto.request.AdminCoordinateRequest;
import com.kospot.admin.presentation.dto.response.AdminCoordinateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Admin Coordinate Api", description = "관리자 - 좌표 관리 API")
@RequestMapping("/admin/coordinates")
public class AdminCoordinateController {

    private final CreateCoordinateUseCase createCoordinateUseCase;
    private final ImportCoordinateByExcelUseCase importCoordinateByExcelUseCase;
    private final FindAllCoordinatesUseCase findAllCoordinatesUseCase;
    private final DeleteCoordinateUseCase deleteCoordinateUseCase;

    @Operation(summary = "좌표 생성", description = "관리자가 폼을 통해 새로운 좌표를 생성합니다.")
    @PostMapping("/")
    public ApiResponseDto<Long> createCoordinate(
            @CurrentMember Long adminId,
            @Valid @RequestBody AdminCoordinateRequest.Create request
    ) {
        Long coordinateId = createCoordinateUseCase.execute(adminId, request);
        return ApiResponseDto.onSuccess(coordinateId);
    }

    @Operation(summary = "좌표 엑셀 업로드", description = "관리자가 엑셀 파일을 통해 좌표를 일괄 등록합니다.")
    @PostMapping(value = "/import-excel", consumes = "multipart/form-data")
    public ApiResponseDto<?> importCoordinateByExcel(
            @CurrentMember Long adminId,
            @RequestParam("file") MultipartFile file
    ) {
        importCoordinateByExcelUseCase.execute(adminId, file);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "좌표 목록 조회", description = "관리자가 전체 좌표 목록을 페이징 조회합니다.")
    @GetMapping("/")
    public ApiResponseDto<Page<AdminCoordinateResponse.CoordinateInfo>> findAllCoordinates(
            @CurrentMember Long adminId,
            @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<AdminCoordinateResponse.CoordinateInfo> coordinates = findAllCoordinatesUseCase.execute(adminId, pageable);
        return ApiResponseDto.onSuccess(coordinates);
    }

    @Operation(summary = "좌표 삭제", description = "관리자가 좌표를 삭제합니다.")
    @DeleteMapping("/{coordinateId}")
    public ApiResponseDto<?> deleteCoordinate(
            @CurrentMember Long adminId,
            @PathVariable("coordinateId") Long coordinateId
    ) {
        deleteCoordinateUseCase.execute(adminId, coordinateId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }
}

