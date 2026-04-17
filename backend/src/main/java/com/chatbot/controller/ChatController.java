package com.chatbot.controller;

import com.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Streaming endpoint — SSE token stream.
     * Use for the chat UI; tokens render progressively.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String sessionId = body.getOrDefault("sessionId", "default");
        log.debug("Chat stream request — sessionId={} message={}", sessionId, message);
        return chatService.streamResponse(message, sessionId);
    }

    /**
     * Blocking endpoint — returns the full response as plain text.
     * Preferred for tool-heavy calls (e.g. add, currentTime) where the model
     * needs several tool round-trips before producing the final answer.
     */
    @PostMapping(value = "/invoke", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> invokeChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String sessionId = body.getOrDefault("sessionId", "default");
        log.debug("Chat invoke request — sessionId={} message={}", sessionId, message);
        return chatService.invokeResponse(message, sessionId);
    }
}
