package com.interview.platform.config;

import com.interview.platform.security.JwtHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Simple in-memory broker for topic subscriptions
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");
        // User-specific channels
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(jwtHandshakeInterceptor)
            .setHandshakeHandler(new org.springframework.web.socket.server.support.DefaultHandshakeHandler() {
                @Override
                protected java.security.Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                                                  org.springframework.web.socket.WebSocketHandler wsHandler,
                                                                  java.util.Map<String, Object> attributes) {
                    String userId = (String) attributes.get("userId");
                    return userId != null ? () -> userId : null;
                }
            })
            .withSockJS();
    }
}
