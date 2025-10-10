package com.kospot.infrastructure.websocket.domain.multi.submission.constants;

public class SubmissionChannelConstants {

    public static final String PREFIX_TOPIC = "/topic/";
    public static final String PREFIX_SUBMISSION = PREFIX_TOPIC + "room/"; // /topic/room/{roomId}/

    public static String getRoadViewPlayerChannel(String roomId) {
        return PREFIX_SUBMISSION + roomId + "/roadview/player/answer"; // 채널을 방 기준? 게임Id기준? round 기준?
    }

}
