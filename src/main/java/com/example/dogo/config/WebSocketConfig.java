package com.example.dogo.config;

import com.example.dogo.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatService chatService;

    @Value("${app.websocket.allowed-origins:http://localhost:8080}")
    private String configuredAllowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/sub");
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins(parseAllowedOrigins(configuredAllowedOrigins))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (accessor.getUser() == null) {
                        throw new IllegalArgumentException("로그인이 필요합니다.");
                    }
                    chatService.authorizeSubscription(accessor.getDestination(), accessor.getUser().getName());
                }
                return message;
            }
        });
    }

    static String[] parseAllowedOrigins(String configuredAllowedOrigins) {
        if (configuredAllowedOrigins == null || configuredAllowedOrigins.isBlank()) {
            throw new IllegalStateException("app.websocket.allowed-origins must contain at least one origin.");
        }

        String[] origins = java.util.Arrays.stream(configuredAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        if (origins.length == 0 || java.util.Arrays.stream(origins).anyMatch("*"::equals)) {
            throw new IllegalStateException("Wildcard WebSocket origins are not allowed.");
        }
        return origins;
    }
}
