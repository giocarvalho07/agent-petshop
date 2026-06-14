package com.mentoriatech.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Serviço principal do agente autônomo do PetShop.
 *
 * Gerencia o loop de Function Calling com o Spring AI + Gemini,
 * mantendo o histórico de conversa por sessão e orquestrando
 * as chamadas às ferramentas (tools) de forma autônoma.
 */
@Slf4j
@Service
public class MedicalAssistantAgent {

    private final ChatClient chatClient;
    private final ChatSessionService sessionService;

    private static final String SYSTEM_PROMPT = """
            Você é um assistente virtual do PetShop PetCare e todas as suas respostas devem se ater ao agendamento de serviços para pets.
            
            Para realizar um agendamento, você precisa saber 4 coisas:
            
            1. Nome do atendente (na maioria das vezes, buscar no database)
            2. Data
            3. Hora
            4. Nome do pet e nome do tutor (dono do pet)
            
            - Tenha certeza que o atendente solicitado pelo tutor está cadastrado no nosso banco de dados.
            - Se o tutor não tiver preferência de atendente, pergunte qual serviço ele deseja para o pet.
            - Se o tutor escolher um serviço, verifique se temos atendentes disponíveis para esse serviço consultando o banco de dados.
            - Se o serviço solicitado não estiver cadastrado no banco de dados, informe que não oferecemos esse serviço e liste os serviços disponíveis.
            - Ao criar o agendamento, use o formato "Tutor: {nome do tutor} | Pet: {nome do pet}" no campo patientName.
            - Nunca solicite dados internos como ID ou ID do calendário ao tutor. Sempre que necessário, consulte diretamente no banco de dados.
            """;

    public MedicalAssistantAgent(ChatClient.Builder chatClientBuilder, ChatSessionService sessionService) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultFunctions(
                        "today",
                        "currentYear",
                        "findVetByName",
                        "getVetsByService",
                        "listServices",
                        "checkAvailability",
                        "createAppointment"
                )
                .build();
        this.sessionService = sessionService;
    }

    /**
     * Envia uma mensagem do usuário para o agente e retorna a resposta.
     * O Spring AI cuida automaticamente de:
     *   1. Enviar o histórico + a nova mensagem para o Gemini
     *   2. Interceptar Function Calls e invocar os @Bean correspondentes
     *   3. Reenviar o resultado da função para o Gemini
     *   4. Repetir até o Gemini gerar uma resposta de texto final
     */
    public String sendMessage(String sessionId, String userMessage) {
        log.debug("sendMessage sessionId={} message={}", sessionId, userMessage);

        // Recupera o histórico da sessão
        List<Message> history = sessionService.getHistory(sessionId);

        // Adiciona a nova mensagem do usuário
        UserMessage userMsg = new UserMessage(userMessage);
        sessionService.addMessage(sessionId, userMsg);

        // Monta a lista de mensagens para o prompt
        List<Message> allMessages = new ArrayList<>(history);

        // Envia para o Gemini com as tools configuradas
        String response = chatClient.prompt()
                .messages(allMessages)
                .call()
                .content();

        // Salva a resposta do assistente no histórico
        sessionService.addMessage(sessionId, new AssistantMessage(response));

        log.debug("Agent response: {}", response);
        return response;
    }
}
