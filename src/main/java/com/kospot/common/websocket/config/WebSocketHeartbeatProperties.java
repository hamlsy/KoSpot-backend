package com.kospot.common.websocket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "websocket")
public class WebSocketHeartbeatProperties {

    private Heartbeat heartbeat = new Heartbeat();

    public Heartbeat getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Heartbeat heartbeat) {
        this.heartbeat = heartbeat;
    }

    public static class Heartbeat {
        private long serverOutgoingMs = 10000L;
        private long serverIncomingMs = 10000L;
        private long deadThresholdMs = 30000L;
        private long gracePeriodMs = 60000L;
        private int schedulerPoolSize = 2;

        public long getServerOutgoingMs() {
            return serverOutgoingMs;
        }

        public void setServerOutgoingMs(long serverOutgoingMs) {
            this.serverOutgoingMs = serverOutgoingMs;
        }

        public long getServerIncomingMs() {
            return serverIncomingMs;
        }

        public void setServerIncomingMs(long serverIncomingMs) {
            this.serverIncomingMs = serverIncomingMs;
        }

        public long getDeadThresholdMs() {
            return deadThresholdMs;
        }

        public void setDeadThresholdMs(long deadThresholdMs) {
            this.deadThresholdMs = deadThresholdMs;
        }

        public long getGracePeriodMs() {
            return gracePeriodMs;
        }

        public void setGracePeriodMs(long gracePeriodMs) {
            this.gracePeriodMs = gracePeriodMs;
        }

        public int getSchedulerPoolSize() {
            return schedulerPoolSize;
        }

        public void setSchedulerPoolSize(int schedulerPoolSize) {
            this.schedulerPoolSize = schedulerPoolSize;
        }
    }
}
