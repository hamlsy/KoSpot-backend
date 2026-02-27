package com.kospot.infrastructure.slack;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Slack Incoming Webhook을 통해 에러 알림 및 회원가입 알림을 비동기 전송하는 컴포넌트.
 * slack.enabled=false인 경우 메시지를 전송하지 않습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotifier {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_STACK_TRACE_LENGTH = 500;

    private final SlackProperties slackProperties;
    private final WebClient.Builder webClientBuilder;

    /**
     * 서버 예외 발생 시 에러 알림 전송
     * 포맷: 🚨 Error at {시각} - {에러메시지} - {StackTrace}
     */
    @Async
    public void sendErrorAlert(String errorMessage, String stackTrace) {
        if (!slackProperties.isEnabled()) {
            return;
        }
        String url = slackProperties.getWebhook().getErrorUrl();
        if (url == null || url.isBlank()) {
            log.warn("[Slack] error webhook URL이 설정되지 않았습니다.");
            return;
        }

        String now = LocalDateTime.now().format(FORMATTER);
        String truncatedTrace = stackTrace != null && stackTrace.length() > MAX_STACK_TRACE_LENGTH
                ? stackTrace.substring(0, MAX_STACK_TRACE_LENGTH) + "..."
                : stackTrace;

        String text = String.format("🚨 Error at %s - %s - %s", now, errorMessage, truncatedTrace);
        send(url, text);
    }

    /**
     * 신규 회원가입 시 알림 전송
     * 포맷: 👤 New user joined at {시각} - ID: {userId}, 이메일: {email}
     */
    @Async
    public void sendRegistrationAlert(Long memberId, String email) {
        if (!slackProperties.isEnabled()) {
            return;
        }
        String url = slackProperties.getWebhook().getRegistrationUrl();
        if (url == null || url.isBlank()) {
            log.warn("[Slack] registration webhook URL이 설정되지 않았습니다.");
            return;
        }

        String now = LocalDateTime.now().format(FORMATTER);
        String text = String.format("👤 New user joined at %s - ID: %d, 이메일: %s", now, memberId, email);
        send(url, text);
    }

    private void send(String webhookUrl, String text) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("text", text);
            webClientBuilder.build()
                    .post()
                    .uri(URI.create(webhookUrl))
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            response -> log.debug("[Slack] 메시지 전송 성공"),
                            error -> log.error("[Slack] 메시지 전송 실패: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("[Slack] 메시지 전송 중 예외 발생: {}", e.getMessage());
        }
    }
}
