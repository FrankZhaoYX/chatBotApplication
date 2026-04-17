package com.chatbot.model;

/**
 * Lightweight projection of a session returned by GET /api/sessions.
 *
 * @param sessionId          unique session identifier
 * @param lastMessagePreview truncated content of the most recent non-system message
 * @param lastRole           "user" | "assistant" — role of the last message
 * @param messageCount       number of user + assistant messages (system excluded)
 */
public record SessionSummary(
        String sessionId,
        String lastMessagePreview,
        String lastRole,
        int messageCount
) {}
