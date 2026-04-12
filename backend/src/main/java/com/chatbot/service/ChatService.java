package com.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;

    // ChatClient.Builder#build() is not a plain field assignment,
    // so @RequiredArgsConstructor is not applicable here.
    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Flux<String> streamResponse(String message, String sessionId) {
        log.debug("Streaming response — sessionId={}", sessionId);
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
