package com.kospot.presentation.notification.controller;

import com.kospot.application.notification.GetMyNotificationsUseCase;
import com.kospot.application.notification.GetUnreadNotificationCountUseCase;
import com.kospot.application.notification.MarkAllNotificationsReadUseCase;
import com.kospot.application.notification.MarkNotificationReadUseCase;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.exception.payload.code.SuccessStatus;
import com.kospot.infrastructure.exception.payload.dto.ApiResponseDto;
import com.kospot.infrastructure.security.aop.CurrentMember;
import com.kospot.presentation.notification.dto.response.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@ApiResponse(responseCode = "2000", description = "OK")
@Tag(name = "Notification Api", description = "알림 API")
@RequestMapping("/notifications")
public class NotificationController {

    private final GetMyNotificationsUseCase getMyNotificationsUseCase;
    private final GetUnreadNotificationCountUseCase getUnreadNotificationCountUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;

    @Operation(summary = "내 알림 목록 조회", description = "내 알림을 최신순으로 조회합니다.")
    @GetMapping
    public ApiResponseDto<List<NotificationResponse.Item>> getMyNotifications(
            @CurrentMember Member member,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "type", required = false) NotificationType type,
            @RequestParam(value = "isRead", required = false) Boolean isRead
    ) {
        return ApiResponseDto.onSuccess(getMyNotificationsUseCase.execute(member, page, size, type, isRead));
    }

    @Operation(summary = "미읽음 알림 개수 조회", description = "내 미읽음 알림 개수를 조회합니다.")
    @GetMapping("/unread-count")
    public ApiResponseDto<NotificationResponse.UnreadCount> getUnreadCount(
            @CurrentMember Member member
    ) {
        return ApiResponseDto.onSuccess(getUnreadNotificationCountUseCase.execute(member));
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @PatchMapping("/{notificationId}/read")
    public ApiResponseDto<?> markRead(
            @CurrentMember Member member,
            @PathVariable("notificationId") Long notificationId
    ) {
        markNotificationReadUseCase.execute(member, notificationId);
        return ApiResponseDto.onSuccess(SuccessStatus._SUCCESS);
    }

    @Operation(summary = "알림 전체 읽음 처리", description = "내 알림을 전체 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ApiResponseDto<NotificationResponse.MarkAllReadResult> markAllRead(
            @CurrentMember Member member
    ) {
        return ApiResponseDto.onSuccess(markAllNotificationsReadUseCase.execute(member));
    }
}
