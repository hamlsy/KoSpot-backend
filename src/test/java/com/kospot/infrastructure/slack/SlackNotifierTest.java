package com.kospot.infrastructure.slack;

import com.kospot.common.slack.SlackNotifier;
import com.kospot.common.slack.SlackProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlackNotifierTest {

    @Mock
    private SlackProperties slackProperties;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private SlackNotifier slackNotifier;

    @BeforeEach
    void setUp() {
        slackNotifier = new SlackNotifier(slackProperties, webClientBuilder);
    }

    @Test
    @DisplayName("slack.enabled=false 이면 에러 알림을 전송하지 않는다")
    void sendErrorAlert_disabled_doesNotSend() {
        // given
        when(slackProperties.isEnabled()).thenReturn(false);

        // when
        slackNotifier.sendErrorAlert("Test error", "at com.kospot.Test.method(Test.java:10)");

        // then
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("webhook error-url이 비어 있으면 에러 알림을 전송하지 않는다")
    void sendErrorAlert_blankUrl_doesNotSend() {
        // given
        SlackProperties.Webhook webhook = new SlackProperties.Webhook();
        webhook.setErrorUrl("");
        when(slackProperties.isEnabled()).thenReturn(true);
        when(slackProperties.getWebhook()).thenReturn(webhook);

        // when
        slackNotifier.sendErrorAlert("Test error", "stacktrace");

        // then
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("slack.enabled=false 이면 회원가입 알림을 전송하지 않는다")
    void sendRegistrationAlert_disabled_doesNotSend() {
        // given
        when(slackProperties.isEnabled()).thenReturn(false);

        // when
        slackNotifier.sendRegistrationAlert(1L, "test@example.com");

        // then
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("webhook registration-url이 비어 있으면 회원가입 알림을 전송하지 않는다")
    void sendRegistrationAlert_blankUrl_doesNotSend() {
        // given
        SlackProperties.Webhook webhook = new SlackProperties.Webhook();
        webhook.setRegistrationUrl("");
        when(slackProperties.isEnabled()).thenReturn(true);
        when(slackProperties.getWebhook()).thenReturn(webhook);

        // when
        slackNotifier.sendRegistrationAlert(1L, "test@example.com");

        // then
        verify(webClientBuilder, never()).build();
    }

    @Test
    @DisplayName("slack.enabled=true && URL 설정 시 에러 알림 메시지가 올바른 포맷으로 WebClient를 통해 전송된다")
    @SuppressWarnings("unchecked")
    void sendErrorAlert_enabled_sendsMessage() {
        // given
        String errorUrl = "https://hooks.slack.com/services/test/error";
        SlackProperties.Webhook webhook = new SlackProperties.Webhook();
        webhook.setErrorUrl(errorUrl);
        when(slackProperties.isEnabled()).thenReturn(true);
        when(slackProperties.getWebhook()).thenReturn(webhook);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        // when
        slackNotifier.sendErrorAlert("NullPointerException", "at com.kospot.Test.method(Test.java:10)");

        // then
        verify(webClient).post();
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString()).isEqualTo(errorUrl);

        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        String text = (String) bodyCaptor.getValue().get("text");
        assertThat(text).startsWith("🚨 Error at");
        assertThat(text).contains("NullPointerException");
    }

    @Test
    @DisplayName("slack.enabled=true && URL 설정 시 회원가입 알림 메시지가 올바른 포맷으로 WebClient를 통해 전송된다")
    @SuppressWarnings("unchecked")
    void sendRegistrationAlert_enabled_sendsMessage() {
        // given
        String registrationUrl = "https://hooks.slack.com/services/test/registration";
        SlackProperties.Webhook webhook = new SlackProperties.Webhook();
        webhook.setRegistrationUrl(registrationUrl);
        when(slackProperties.isEnabled()).thenReturn(true);
        when(slackProperties.getWebhook()).thenReturn(webhook);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        // when
        slackNotifier.sendRegistrationAlert(42L, "newuser@kospot.com");

        // then
        verify(webClient).post();
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestBodyUriSpec).uri(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString()).isEqualTo(registrationUrl);

        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(requestBodySpec).bodyValue(bodyCaptor.capture());
        String text = (String) bodyCaptor.getValue().get("text");
        assertThat(text).startsWith("👤 New user joined at");
        assertThat(text).contains("ID: 42");
        assertThat(text).contains("newuser@kospot.com");
    }
}
