package com.kospot.application.admin.notification;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.application.service.MemberService;
import com.kospot.domain.notification.model.NotificationCreateCommand;
import com.kospot.domain.notification.model.NotificationData;
import com.kospot.domain.notification.port.NotificationStore;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.notification.service.NotificationPushService;
import com.kospot.presentation.admin.dto.request.AdminNotificationRequest;
import com.kospot.presentation.notification.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class SendAdminMessageUseCase {

    private final MemberService memberService;
    private final MemberAdaptor memberAdaptor;
    private final NotificationStore notificationStore;
    private final NotificationPushService notificationPushService;

    public int execute(Long adminId, AdminNotificationRequest.SendMessage request) {
        Member admin = memberAdaptor.queryById(adminId);
        memberService.validateAdmin(admin);

        List<Long> targetMemberIds = resolveTargets(request);

        String payloadJson = String.format(
                "{\"adminId\":%d}",
                admin.getId()
        );

        NotificationMessage pushMessage = NotificationMessage.builder()
                .notificationId(null)
                .type(NotificationType.ADMIN_MESSAGE.name())
                .title(request.getTitle())
                .content(request.getContent())
                .payloadJson(payloadJson)
                .sourceId(null)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        if (request.getTargetType() == AdminNotificationRequest.TargetType.ALL) {
            // 전체 발송: Redis fan-out + 전역 푸시
            List<NotificationCreateCommand> commands = targetMemberIds.stream()
                    .map(memberId -> new NotificationCreateCommand(
                            memberId,
                            NotificationType.ADMIN_MESSAGE,
                            request.getTitle(),
                            request.getContent(),
                            payloadJson,
                            null
                    ))
                    .toList();
            notificationStore.saveAll(commands);
            notificationPushService.sendGlobal(pushMessage);
        } else {
            // 선택 발송: 사용자별로 생성해서 notificationId 포함 푸시
            for (Long memberId : targetMemberIds) {
                NotificationData notification = notificationStore.save(new NotificationCreateCommand(
                        memberId,
                        NotificationType.ADMIN_MESSAGE,
                        request.getTitle(),
                        request.getContent(),
                        payloadJson,
                        null
                ));
                notificationPushService.sendToMember(memberId, NotificationMessage.from(notification));
            }
        }

        log.info("Admin notification sent - AdminId: {}, TargetType: {}, TargetCount: {}",
                admin.getId(), request.getTargetType(), targetMemberIds.size());

        return targetMemberIds.size();
    }

    private List<Long> resolveTargets(AdminNotificationRequest.SendMessage request) {
        if (request.getTargetType() == AdminNotificationRequest.TargetType.ALL) {
            return memberAdaptor.findAll().stream()
                    .map(Member::getId)
                    .toList();
        }

        List<Long> memberIds = request.getMemberIds();
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }

        // 존재 검증(요청에 잘못된 ID가 들어오면 즉시 실패)
        List<Long> targets = new ArrayList<>(memberIds.size());
        for (Long memberId : memberIds) {
            memberAdaptor.queryById(memberId);
            targets.add(memberId);
        }
        return targets;
    }
}
