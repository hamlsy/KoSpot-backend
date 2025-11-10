package com.kospot.infrastructure.websocket.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingLeaveContext {
    private Long roomId;
    private String reason;
    private long expiresAt;
    private String sessionVersion;
}
