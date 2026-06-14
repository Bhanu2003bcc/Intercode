package com.interview.platform.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.platform.models.RoomMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            RoomMessage roomMessage = objectMapper.readValue(message.getBody(), RoomMessage.class);
            String roomToken = roomMessage.getRoomToken();

            // Route to targeted peer or broadcast to entire room
            if (roomMessage.getTargetPeerId() != null && !roomMessage.getTargetPeerId().isBlank()) {
                messagingTemplate.convertAndSendToUser(
                    roomMessage.getTargetPeerId(),
                    "/queue/room/" + roomToken,
                    roomMessage
                );
            } else {
                messagingTemplate.convertAndSend("/topic/room/" + roomToken, roomMessage);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis message: {}", e.getMessage());
        }
    }
}
