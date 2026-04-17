package com.chatbot.service;

import com.chatbot.entity.ChatMessage;
import com.chatbot.entity.MessageRole;
import com.chatbot.model.SessionSummary;
import com.chatbot.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    @Value("${chat.system-prompt:You are a helpful assistant.}")
    private String systemPrompt;

    @Value("${chat.history.max-messages:20}")
    private int maxMessages;

    private final ChatMessageRepository chatMessageRepository;

    // In-memory cache — rebuilt from DB on cache miss (e.g. after restart)
    private final ConcurrentHashMap<String, List<Message>> sessions = new ConcurrentHashMap<>();

    /**
     * Returns the full history for a session.
     * On cache miss, loads persisted messages from the DB before returning.
     */
    public List<Message> getOrCreateHistory(String sessionId) {
        return sessions.computeIfAbsent(sessionId, this::loadOrCreate);
    }

    public void addUserMessage(String sessionId, String content) {
        getOrCreateHistory(sessionId).add(new UserMessage(content));
        trim(getOrCreateHistory(sessionId));
        persist(sessionId, MessageRole.USER, content);
        log.debug("Session {} — user message saved", sessionId);
    }

    public void addAssistantMessage(String sessionId, String content) {
        getOrCreateHistory(sessionId).add(new AssistantMessage(content));
        trim(getOrCreateHistory(sessionId));
        persist(sessionId, MessageRole.ASSISTANT, content);
        log.debug("Session {} — assistant message saved", sessionId);
    }

    /**
     * Returns summaries for all sessions — merges the in-memory cache (current JVM)
     * with the DB (survives restarts).
     */
    public List<SessionSummary> getAllSummaries() {
        Set<String> allIds = new HashSet<>(chatMessageRepository.findDistinctSessionIds());
        allIds.addAll(sessions.keySet());

        return allIds.stream()
                .map(sessionId -> {
                    String preview = chatMessageRepository
                            .findTopBySessionIdOrderByTimestampDesc(sessionId)
                            .map(m -> truncate(m.getContent(), 80))
                            .orElse("");
                    String role = chatMessageRepository
                            .findTopBySessionIdOrderByTimestampDesc(sessionId)
                            .map(m -> m.getRole().name().toLowerCase())
                            .orElse("");
                    int count = (int) chatMessageRepository.countBySessionId(sessionId);
                    return new SessionSummary(sessionId, preview, role, count);
                })
                .sorted(Comparator.comparing(SessionSummary::sessionId))
                .toList();
    }

    @Transactional
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        log.debug("Session {} cleared", sessionId);
    }

    // --- private helpers ---

    private List<Message> loadOrCreate(String sessionId) {
        List<Message> history = new ArrayList<>();
        history.add(new SystemMessage(systemPrompt));

        List<ChatMessage> persisted =
                chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        persisted.stream().map(this::toSpringMessage).forEach(history::add);

        if (!persisted.isEmpty()) {
            log.debug("Session {} — restored {} messages from DB", sessionId, persisted.size());
        } else {
            log.debug("Session {} — new session created", sessionId);
        }
        return history;
    }

    private Message toSpringMessage(ChatMessage msg) {
        return switch (msg.getRole()) {
            case USER -> new UserMessage(msg.getContent());
            case ASSISTANT -> new AssistantMessage(msg.getContent());
        };
    }

    private void persist(String sessionId, MessageRole role, String content) {
        chatMessageRepository.save(ChatMessage.builder()
                .sessionId(sessionId)
                .role(role)
                .content(content)
                .timestamp(Instant.now())
                .build());
    }

    private void trim(List<Message> history) {
        while (history.size() > maxMessages + 1) {  // +1 for system message
            history.remove(1);                        // drop oldest non-system message
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
