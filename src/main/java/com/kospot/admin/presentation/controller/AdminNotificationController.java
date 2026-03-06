package com.kospot.admin.presentation.controller;

import com.kospot.admin.application.usecase.notification.SendAdminMessageUseCase;
import com.kospot.common.exception.payload.dto.ApiResponseDto;
import com.kospot.common.security.aop.CurrentMember;
import com.kospot.admin.presentation.dto.request.AdminNotificationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Admin Notification Api", description = "관리자 - 알림 관리 API")
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final SendAdminMessageUseCase sendAdminMessageUseCase;

    @Operation(summary = "관리자 메시지 발송", description = "관리자 메시지를 전체/선택 사용자에게 발송합니다.")
    @PostMapping("/messages")
    public ApiResponseDto<Map<String, Object>> sendAdminMessage(
            @CurrentMember Long adminId,
            @Valid @RequestBody AdminNotificationRequest.SendMessage request
    ) {
        int sentCount = sendAdminMessageUseCase.execute(adminId, request);
        return ApiResponseDto.onSuccess(Map.of("sentCount", sentCount));
    }
}
