package com.interview.platform.security;

import com.interview.platform.service.JwtService;
import com.interview.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        if (query != null) {
            String token = null;
            String guestId = null;
            String guestName = null;
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                } else if (param.startsWith("guestId=")) {
                    guestId = param.substring(8);
                } else if (param.startsWith("guestName=")) {
                    try {
                        guestName = java.net.URLDecoder.decode(param.substring(10), java.nio.charset.StandardCharsets.UTF_8.name());
                    } catch (Exception e) {
                        guestName = param.substring(10);
                    }
                }
            }
            if (token != null && !token.equals("null") && !token.isBlank()) {
                try {
                    String email = jwtService.extractEmail(token);
                    userRepository.findByEmail(email).ifPresent(user -> {
                        attributes.put("userId", user.getId().toString());
                        attributes.put("userEmail", user.getEmail());
                        attributes.put("userRole", user.getRole().name());
                        attributes.put("fullName", user.getFullName());
                    });
                } catch (Exception e) {
                    // Invalid token
                }
            }
            if (!attributes.containsKey("userId")) {
                String finalGuestId = (guestId != null && !guestId.equals("null") && !guestId.isBlank())
                    ? guestId : "guest_" + java.util.UUID.randomUUID().toString().substring(0, 8);
                String finalGuestName = (guestName != null && !guestName.equals("null") && !guestName.isBlank())
                    ? guestName : "Guest_" + finalGuestId.substring(Math.min(6, finalGuestId.length()));
                attributes.put("userId", finalGuestId);
                attributes.put("fullName", finalGuestName);
                attributes.put("userRole", "GUEST");
            }
        } else {
            String finalGuestId = "guest_" + java.util.UUID.randomUUID().toString().substring(0, 8);
            attributes.put("userId", finalGuestId);
            attributes.put("fullName", "Guest_" + finalGuestId.substring(Math.min(6, finalGuestId.length())));
            attributes.put("userRole", "GUEST");
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {}
}
