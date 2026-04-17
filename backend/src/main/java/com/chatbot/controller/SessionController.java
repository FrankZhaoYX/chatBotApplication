package com.chatbot.controller;

import com.chatbot.model.SessionSummary;
import com.chatbot.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public List<SessionSummary> getSessions() {
        log.debug("GET /api/sessions");
        return sessionService.getAllSummaries();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        log.debug("DELETE /api/sessions/{}", sessionId);
        sessionService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
