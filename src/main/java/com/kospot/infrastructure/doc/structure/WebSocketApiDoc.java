package com.kospot.infrastructure.doc.structure;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class WebSocketApiDoc {
    private ApiInfo info;
    private List<ServerInfo> servers;
    private Map<String, ChannelInfo> channels;

    @Data
    @Builder
    static class ApiInfo {
        private String title;
        private String version;
        private String description;
    }

    @Data
    @Builder
    class ServerInfo {
        private String url;
        private String protocol;
    }

    @Data
    @Builder
    class ChannelInfo {
        private OperationInfo subscribe;
        private OperationInfo publish;
    }

    @Data
    @Builder
    class OperationInfo {
        private String description;
        private String trigger;
        private MessageInfo message;
    }

    @Data
    @Builder
    class MessageInfo {
        private Object payload;
    }

}


