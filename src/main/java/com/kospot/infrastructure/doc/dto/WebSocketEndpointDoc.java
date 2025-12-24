package com.kospot.infrastructure.doc.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WebSocketEndpointDoc {
    private String destination;
    private String description;
    // 양방향 지원
    private ClientToServerInfo clientToServer;
    private ServerToClientInfo serverToClient;

    @Data
    @Builder
    public static class ClientToServerInfo {
        private String payloadType;
        private Object payloadExample;
        private String description;
    }

    @Data
    @Builder
    public static class ServerToClientInfo {
        private String payloadType;
        private Object payloadExample;
        private String description;
        private String trigger; // 어떤 상황에 브로드캐스트되는지
    }
}