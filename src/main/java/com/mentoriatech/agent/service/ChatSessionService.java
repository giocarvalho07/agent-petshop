package com.mentoriatech.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia sessões de conversa em memória.
 *
 * Substitui o campo memory []*genai.Content do Go.
 * Em produção, trocar por Redis ou persistência em banco.
 */
@Slf4j
@Service
public class ChatSessionService {

    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new ArrayList<>());
        log.debug("Nova sessão criada: {}", sessionId);
        return sessionId;
    }

    public List<Message> getHistory(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> {
            log.debug("Sessão não encontrada, criando nova: {}", sessionId);
            return new ArrayList<>();
        });
    }

    public void addMessage(String sessionId, Message message) {
        getHistory(sessionId).add(message);
    }

    public void addMessages(String sessionId, List<Message> messages) {
        getHistory(sessionId).addAll(messages);
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }
}
