package com.interview.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.platform.models.RoomMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Unified room message handler — all message types flow through here.
     * Publishes to Redis so all horizontally scaled nodes receive it.
     */
    @MessageMapping("/room/{roomToken}")
    public void handleRoomMessage(@DestinationVariable String roomToken,
                                  @Payload RoomMessage message,
                                  SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();

        // Enrich message with sender identity from WebSocket session
        if (attrs != null) {
            message.setSenderId((String) attrs.getOrDefault("userId", "unknown"));
            message.setSenderName((String) attrs.getOrDefault("fullName", "Anonymous"));
        }
        message.setRoomToken(roomToken);
        message.setTimestamp(System.currentTimeMillis());

        // Skip PING messages from being relayed
        if ("PING".equals(message.getType())) {
            return;
        }

        // Publish to Redis channel for cross-node delivery
        redisTemplate.convertAndSend("room:" + roomToken, message);
    }
}
