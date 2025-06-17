package com.kospot.infrastructure.config.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> ALLOWED_ORIGINS;

    public WebSocketConfig(@Value("${websocket.allowed-origins}") List<String> ALLOWED_ORIGINS) {
        this.ALLOWED_ORIGINS = ALLOWED_ORIGINS;
    }

    // STOMP Endpoints
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 로비 및 개임 내 통신
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ALLOWED_ORIGINS.toArray(new String[0]))
                .withSockJS();
        log.info("WebSocket STOMP endpoint registered at /ws");

        // 전역 알림 - 시스템 공지사항
        registry.addEndpoint("/ws/notification")
                .setAllowedOriginPatterns(ALLOWED_ORIGINS.toArray(new String[0]))
                .withSockJS();
    }

    /**
     * 메시지 브로커 설정
     * Simple Broker를 사용하여 메모리 기반 메시지 브로킹 제공
     * todo 프로덕션 환경에서는 Redis Pub/Sub으로 확장 가능
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 목적지 프리픽스 설정
        config.enableSimpleBroker(
                "/topic",    // 다대다 브로드캐스트 (게임 룸 전체 메시지)
                "/queue"     // 일대일 개인 메시지 (개별 플레이어 타겟팅)
        );

        // 클라이언트가 메시지를 전송할 때 사용할 프리픽스
        config.setApplicationDestinationPrefixes("/app");

        // 개인 메시지 목적지 프리픽스 설정
        config.setUserDestinationPrefix("/user");

    }

    /**
     * WebSocket 전송 설정 최적화
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // 메시지 크기 제한 (32KB) - 게임 데이터 전송 최적화
        registry.setMessageSizeLimit(32 * 1024);

        // 첫 메시지 수신 타임아웃 (30초)
        registry.setTimeToFirstMessage(30000);

        // Send 버퍼 크기 제한 (512KB) - 동시 다발적 메시지 처리
        registry.setSendBufferSizeLimit(512 * 1024);
    }

    //ec2 single core instance 설정 반영
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(2)    // 기본 스레드 수
                .maxPoolSize(4)     // 최대 스레드 수
                .keepAliveSeconds(60);  // 유휴 스레드 유지 시간(초)
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(2)
                .maxPoolSize(4)
                .keepAliveSeconds(60);
    }
}
