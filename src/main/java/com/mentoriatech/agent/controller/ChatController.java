package com.mentoriatech.agent.controller;

import com.mentoriatech.agent.dto.ChatRequest;
import com.mentoriatech.agent.dto.ChatResponse;
import com.mentoriatech.agent.service.ChatSessionService;
import com.mentoriatech.agent.service.MedicalAssistantAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para o assistente de agendamento do PetShop.
 *
 * Substitui o loop CLI de main.go por endpoints HTTP,
 * permitindo integração com frontends web, mobile, ou WhatsApp.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final MedicalAssistantAgent agent;
    private final ChatSessionService sessionService;

    /**
     * POST /api/chat
     * Envia uma mensagem para o assistente e recebe a resposta.
     *
     * Body: { "sessionId": "uuid-opcional", "userMessage": "Quero agendar um banho para meu pet" }
     * Response: { "replyText": "Olá! Posso te ajudar...", "sessionId": "uuid" }
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId();

        // Se não vier sessionId, cria uma nova sessão
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = sessionService.createSession();
            log.info("Nova sessão iniciada: {}", sessionId);
        }

        String reply = agent.sendMessage(sessionId, request.userMessage());
        return ResponseEntity.ok(new ChatResponse(reply, sessionId));
    }

    /**
     * POST /api/session
     * Cria uma nova sessão de conversa e retorna o sessionId.
     */
    @PostMapping("/session")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = sessionService.createSession();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    /**
     * GET /api/health
     * Endpoint de health check para monitoramento.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "petshop-agent"));
    }
}
