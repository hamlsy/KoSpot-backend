package com.kospot.infrastructure.websocket.config;

import com.kospot.infrastructure.websocket.interceptor.ChatChannelInterceptor;
import lombok.RequiredArgsConstructor;
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
    private final ChatChannelInterceptor chatChannelInterceptor;

    public WebSocketConfig(@Value("${websocket.allowed-origins}") List<String> ALLOWED_ORIGINS,
                           ChatChannelInterceptor chatChannelInterceptor) {
        this.ALLOWED_ORIGINS = ALLOWED_ORIGINS;
        this.chatChannelInterceptor = chatChannelInterceptor;
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


    //Simple Broker Settings
    //todo 프로덕션 환경에서는 Redis Pub/Sub으로 확장 가능
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

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(32 * 1024);
        registry.setTimeToFirstMessage(30000);
        registry.setSendBufferSizeLimit(512 * 1024);
    }

    //ec2 single core instance 설정 반영
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatChannelInterceptor);
        registration.taskExecutor()
                .corePoolSize(2)
                .maxPoolSize(4)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(2)
                .maxPoolSize(4)
                .keepAliveSeconds(60);
    }
}
