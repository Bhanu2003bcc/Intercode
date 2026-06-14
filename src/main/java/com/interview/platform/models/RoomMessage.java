package com.interview.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoomMessage {
    // Event types: WEBRTC_OFFER, WEBRTC_ANSWER, ICE_CANDIDATE,
    //              CODE_SYNC, CHAT, CURSOR, USER_JOIN, USER_LEAVE,
    //              PANEL_RESIZE, EXECUTION_RESULT, PING
    private String type;
    private String roomToken;
    private String senderId;
    private String senderName;
    private String targetPeerId;  // null = broadcast to all
    private Object payload;       // varies by type
    private long timestamp;
}
