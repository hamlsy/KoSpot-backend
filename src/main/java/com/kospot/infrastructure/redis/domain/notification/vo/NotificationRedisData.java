package com.kospot.infrastructure.redis.domain.notification.vo;

public class NotificationRedisData {

    private Long notificationId;
    private Long receiverMemberId;
    private String type;
    private String title;
    private String content;
    private String payloadJson;
    private Long sourceId;
    private boolean isRead;
    private Long readAtMillis;
    private long createdAtMillis;

    public NotificationRedisData() {
    }

    public NotificationRedisData(
            Long notificationId,
            Long receiverMemberId,
            String type,
            String title,
            String content,
            String payloadJson,
            Long sourceId,
            boolean isRead,
            Long readAtMillis,
            long createdAtMillis
    ) {
        this.notificationId = notificationId;
        this.receiverMemberId = receiverMemberId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.payloadJson = payloadJson;
        this.sourceId = sourceId;
        this.isRead = isRead;
        this.readAtMillis = readAtMillis;
        this.createdAtMillis = createdAtMillis;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getReceiverMemberId() {
        return receiverMemberId;
    }

    public void setReceiverMemberId(Long receiverMemberId) {
        this.receiverMemberId = receiverMemberId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Long getReadAtMillis() {
        return readAtMillis;
    }

    public void setReadAtMillis(Long readAtMillis) {
        this.readAtMillis = readAtMillis;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public void setCreatedAtMillis(long createdAtMillis) {
        this.createdAtMillis = createdAtMillis;
    }
}
