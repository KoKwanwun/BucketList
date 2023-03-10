package com.team9.bucket_list.config;

import com.team9.bucket_list.security.filter.ChatErrorHandler;
import com.team9.bucket_list.security.filter.ChatPreHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatPreHandler chatPreHandler;
    private final ChatErrorHandler chatErrorHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //stomp의 접속 주소
        registry.addEndpoint("/ws").setAllowedOriginPatterns("https://www.ourbucketlist.link").withSockJS();
        registry.setErrorHandler(chatErrorHandler);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //클라이언트의 send요청 처리
        registry.setApplicationDestinationPrefixes("/pub");
        //sub하는 클라이언트에게 메시지 전달
        registry.enableSimpleBroker("/sub","/sub/list");

    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(chatPreHandler);
    }
}
