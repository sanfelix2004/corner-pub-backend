package com.corner.pub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use /topic for broadcasting messages to clients
        config.enableSimpleBroker("/topic");

        // Use /app for messages originating from clients routed to controllers
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint the frontend connects to. We add setAllowedOriginPatterns for
        // CORS.
        registry.addEndpoint("/ws-orders")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback for older browsers
    }
}
