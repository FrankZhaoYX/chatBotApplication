package com.chatbot.model;

/**
 * Lightweight DTO stored in Redis to represent a single chat message.
 * Using a plain record avoids coupling Redis serialization to Spring AI's internal types.
 *
 * @param role    "system" | "user" | "assistant"
 * @param content message text
 */
public record MessageDTO(String role, String content) {}
