package com.eps.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket & STOMP Message Broker Configuration
 * Powers real-time notifications and LIFO purchase request streaming
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory message broker for subscription topics
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages routed to @MessageMapping handler methods
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket handshake endpoint with SockJS fallback
        registry.addEndpoint("/ws-procurement")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Plain WebSocket endpoint
        registry.addEndpoint("/ws-procurement")
                .setAllowedOriginPatterns("*");
    }
}
