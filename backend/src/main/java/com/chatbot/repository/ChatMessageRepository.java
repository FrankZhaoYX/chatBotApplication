package com.chatbot.repository;

import com.chatbot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    Optional<ChatMessage> findTopBySessionIdOrderByTimestampDesc(String sessionId);

    long countBySessionId(String sessionId);

    @Query("SELECT DISTINCT cm.sessionId FROM ChatMessage cm ORDER BY cm.sessionId")
    List<String> findDistinctSessionIds();

    void deleteBySessionId(String sessionId);
}
