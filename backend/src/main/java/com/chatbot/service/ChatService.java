package com.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final SessionService sessionService;

    public ChatService(ChatClient.Builder builder,
                       ToolCallbackProvider toolCallbackProvider,
                       SessionService sessionService) {
        // Register MCP tools as defaults so every prompt — stream or blocking — has them
        this.chatClient = builder
                .defaultTools(toolCallbackProvider)
                .build();
        this.sessionService = sessionService;
    }

    /**
     * Streaming path — tokens arrive in real time.
     * Spring AI handles tool round-trips internally before resuming the token stream.
     * Best for chat UI where progressive rendering matters.
     */
    public Flux<String> streamResponse(String message, String sessionId) {
        log.debug("Stream request — sessionId={}", sessionId);

        sessionService.addUserMessage(sessionId, message);
        List<Message> history = Objects.requireNonNull(sessionService.getOrCreateHistory(sessionId));
        StringBuilder accumulated = new StringBuilder();

        return chatClient.prompt()
                .messages(history)
                .stream()
                .content()
                .doOnNext(accumulated::append)
                .doOnComplete(() ->
                        sessionService.addAssistantMessage(sessionId, accumulated.toString())
                )
                .doOnError(err ->
                        log.error("Stream error — sessionId={}: {}", sessionId, err.getMessage())
                );
    }

    /**
     * Blocking path — waits for the full response including all tool round-trips.
     * Wrapped in boundedElastic so it never blocks a Reactor event-loop thread.
     * Best for tool-heavy requests where latency matters less than reliability.
     */
    public Mono<String> invokeResponse(String message, String sessionId) {
        log.debug("Invoke request — sessionId={}", sessionId);

        sessionService.addUserMessage(sessionId, message);
        List<Message> history = Objects.requireNonNull(sessionService.getOrCreateHistory(sessionId));

        return Mono.fromCallable(() ->
                        chatClient.prompt()
                                .messages(history)
                                .call()
                                .content()
                )
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(response ->
                        sessionService.addAssistantMessage(sessionId, response)
                )
                .doOnError(err ->
                        log.error("Invoke error — sessionId={}: {}", sessionId, err.getMessage())
                );
    }
}
