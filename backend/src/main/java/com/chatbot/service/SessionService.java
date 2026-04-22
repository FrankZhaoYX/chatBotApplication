package com.chatbot.service;

import com.chatbot.entity.ChatMessage;
import com.chatbot.entity.MessageRole;
import com.chatbot.model.MessageDTO;
import com.chatbot.model.SessionSummary;
import com.chatbot.repository.ChatMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    @Value("${chat.system-prompt:You are a helpful assistant.}")
    private String systemPrompt;

    @Value("${chat.history.max-messages:20}")
    private int maxMessages;

    @Value("${chat.session.redis-ttl-minutes:60}")
    private int redisTtlMinutes;

    private static final String KEY_PREFIX = "chat:session:";

    private final ChatMessageRepository chatMessageRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Returns the full history for a session as Spring AI Message objects.
     * Reads from Redis; on miss, rebuilds from DB and primes the cache.
     */
    public List<Message> getOrCreateHistory(String sessionId) {
        return toSpringMessages(getOrCreateDTOs(sessionId));
    }

    public void addUserMessage(String sessionId, String content) {
        addUserAndGetHistory(sessionId, content);
    }

    /**
     * Adds the user message and returns the updated history in one Redis round-trip.
     * Use this in ChatService to avoid a redundant read after the write.
     */
    public List<Message> addUserAndGetHistory(String sessionId, String content) {
        List<MessageDTO> history = getOrCreateDTOs(sessionId);
        history.add(new MessageDTO("user", content));
        trimDTOs(history);
        saveDTOs(sessionId, history);
        persist(sessionId, MessageRole.USER, content);
        log.debug("Session {} — user message saved", sessionId);
        return toSpringMessages(history);
    }

    public void addAssistantMessage(String sessionId, String content) {
        List<MessageDTO> history = getOrCreateDTOs(sessionId);
        history.add(new MessageDTO("assistant", content));
        trimDTOs(history);
        saveDTOs(sessionId, history);
        persist(sessionId, MessageRole.ASSISTANT, content);
        log.debug("Session {} — assistant message saved", sessionId);
    }

    /**
     * Returns summaries for all sessions — merges DB session IDs with Redis keys.
     */
    public List<SessionSummary> getAllSummaries() {
        Set<String> allIds = new HashSet<>(chatMessageRepository.findDistinctSessionIds());

        // Include sessions that only exist in Redis (no DB row yet)
        // Note: KEYS is O(N) — acceptable for a low-volume chat app; swap to SCAN if needed
        Set<String> redisKeys = redisTemplate.keys(KEY_PREFIX + "*");
        if (redisKeys != null) {
            redisKeys.forEach(key -> allIds.add(key.substring(KEY_PREFIX.length())));
        }

        return allIds.stream()
                .map(sessionId -> {
                    var last = chatMessageRepository
                            .findTopBySessionIdOrderByTimestampDesc(sessionId);
                    String preview = last.map(m -> truncate(m.getContent(), 80)).orElse("");
                    String role    = last.map(m -> m.getRole().name().toLowerCase()).orElse("");
                    int count = (int) chatMessageRepository.countBySessionId(sessionId);
                    return new SessionSummary(sessionId, preview, role, count);
                })
                .sorted(Comparator.comparing(SessionSummary::sessionId))
                .toList();
    }

    @Transactional
    public void clearSession(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        log.debug("Session {} cleared", sessionId);
    }

    // --- private helpers ---

    private List<MessageDTO> getOrCreateDTOs(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                List<MessageDTO> dtos = objectMapper.readValue(json, new TypeReference<>() {});
                // Refresh TTL on every access so active sessions don't expire mid-conversation
                redisTemplate.expire(key, Duration.ofMinutes(redisTtlMinutes));
                return new ArrayList<>(dtos);
            } catch (JsonProcessingException e) {
                log.warn("Session {} — Redis deserialization failed, rebuilding from DB", sessionId, e);
            }
        }
        return loadFromDB(sessionId);
    }

    private List<MessageDTO> loadFromDB(String sessionId) {
        List<MessageDTO> history = new ArrayList<>();
        history.add(new MessageDTO("system", systemPrompt));

        List<ChatMessage> persisted =
                chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        persisted.stream()
                .map(m -> new MessageDTO(m.getRole().name().toLowerCase(), m.getContent()))
                .forEach(history::add);

        saveDTOs(sessionId, history);

        if (!persisted.isEmpty()) {
            log.debug("Session {} — restored {} messages from DB into Redis", sessionId, persisted.size());
        } else {
            log.debug("Session {} — new session created", sessionId);
        }
        return history;
    }

    private void saveDTOs(String sessionId, List<MessageDTO> dtos) {
        try {
            String json = objectMapper.writeValueAsString(dtos);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + sessionId, json, Duration.ofMinutes(redisTtlMinutes));
        } catch (JsonProcessingException e) {
            log.error("Session {} — failed to serialize history to Redis", sessionId, e);
        }
    }

    private List<Message> toSpringMessages(List<MessageDTO> dtos) {
        return dtos.stream().map(this::toSpringMessage).collect(Collectors.toList());
    }

    private Message toSpringMessage(MessageDTO dto) {
        return switch (dto.role()) {
            case "user"      -> new UserMessage(dto.content());
            case "assistant" -> new AssistantMessage(dto.content());
            default          -> new SystemMessage(dto.content());
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

    private void trimDTOs(List<MessageDTO> history) {
        while (history.size() > maxMessages + 1) {  // +1 for system message
            history.remove(1);                        // drop oldest non-system message
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
