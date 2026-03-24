package com.mentoriatech.agent.tools;

import com.mentoriatech.agent.dto.*;
import com.mentoriatech.agent.entity.Vet;
import com.mentoriatech.agent.repository.VetRepository;
import com.mentoriatech.agent.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Configuração das ferramentas (Tools) disponíveis para o agente de IA do PetShop.
 *
 * Cada @Bean Function aqui é automaticamente registrado pelo Spring AI
 * como uma ferramenta que o modelo Gemini pode invocar durante a conversa.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentToolsConfig {

    private final VetRepository vetRepository;
    private final GoogleCalendarService calendarService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // =========================================================================
    // Tool: today - Retorna a data e hora atuais
    // =========================================================================
    @Bean
    @Description("Retorna a data e hora atuais no formato yyyy-MM-dd HH:mm")
    public Supplier<Map<String, String>> today() {
        return () -> {
            String now = LocalDateTime.now().format(DATE_FORMAT);
            log.debug("[Tool] today -> {}", now);
            return Map.of("date", now);
        };
    }

    // =========================================================================
    // Tool: currentYear - Retorna o ano atual
    // =========================================================================
    @Bean
    @Description("Retorna o ano atual")
    public Supplier<Map<String, String>> currentYear() {
        return () -> {
            String year = String.valueOf(LocalDateTime.now().getYear());
            log.debug("[Tool] currentYear -> {}", year);
            return Map.of("year", year);
        };
    }

    // =========================================================================
    // Tool: findVetByName - Busca um atendente/veterinário pelo nome no banco
    // =========================================================================
    @Bean
    @Description("Busca um atendente ou veterinário do PetShop pelo nome no banco de dados. Retorna ID, nome, serviço (specialty) e ID do calendário.")
    public Function<FindVetRequest, VetResponse> findVetByName() {
        return request -> {
            log.debug("[Tool] findVetByName name={}", request.name());

            return vetRepository.findByNameIgnoreCase(request.name())
                    .map(v -> new VetResponse(v.getId(), v.getName(), v.getSpecialty(), v.getCalendarId(), "Sucesso"))
                    .orElse(new VetResponse(null, null, null, null, "Atendente não encontrado"));
        };
    }

    // =========================================================================
    // Tool: getVetsByService - Lista atendentes por serviço/especialidade
    // =========================================================================
    @Bean
    @Description("Retorna uma lista dos atendentes do PetShop cadastrados para o serviço informado. Os serviços disponíveis são: Banho & Tosa, Consultas e Pronto Socorro.")
    public Function<SpecialtyRequest, List<VetResponse>> getVetsByService() {
        return request -> {
            log.debug("[Tool] getVetsByService service={}", request.specialty());

            List<Vet> vets = vetRepository.findBySpecialtyIgnoreCase(request.specialty());

            if (vets.isEmpty()) {
                return List.of(new VetResponse(null, null, null, null, "Nenhum atendente encontrado para esse serviço"));
            }

            return vets.stream()
                    .map(v -> new VetResponse(v.getId(), v.getName(), v.getSpecialty(), v.getCalendarId(), "Sucesso"))
                    .toList();
        };
    }

    // =========================================================================
    // Tool: listServices - Lista todos os serviços disponíveis no PetShop
    // =========================================================================
    @Bean
    @Description("Lista todos os serviços disponíveis no PetShop cadastrados no banco de dados.")
    public Supplier<Map<String, List<String>>> listServices() {
        return () -> {
            List<String> services = vetRepository.findDistinctSpecialties();
            log.debug("[Tool] listServices -> {}", services);
            return Map.of("services", services);
        };
    }

    // =========================================================================
    // Tool: checkAvailability - Verifica disponibilidade no calendário
    // =========================================================================
    @Bean
    @Description("Verifica se determinada data está disponível no calendário de um atendente do PetShop para agendamento de serviço. Recebe o ID do calendário e a data no formato yyyy-MM-dd HH:mm.")
    public Function<AvailabilityRequest, Map<String, Boolean>> checkAvailability() {
        return request -> {
            log.debug("[Tool] checkAvailability calendarId={} date={}", request.calendarId(), request.date());

            LocalDateTime dateTime = LocalDateTime.parse(request.date(), DATE_FORMAT);
            boolean available = calendarService.checkAvailability(request.calendarId(), dateTime);

            return Map.of("available", available);
        };
    }

    // =========================================================================
    // Tool: createAppointment - Cria agendamento no calendário do atendente
    // =========================================================================
    @Bean
    @Description("Cria um novo agendamento de serviço no PetShop. Passar calendarId (do atendente), patientName (nome do tutor e do pet, ex: 'Tutor: Giovanni | Pet: Rex'), specialty (serviço escolhido) e date no formato yyyy-MM-dd HH:mm.")
    public Function<AppointmentRequest, Map<String, String>> createAppointment() {
        return request -> {
            log.debug("[Tool] createAppointment calendarId={} patientName={} specialty={} date={}",
                    request.calendarId(), request.patientName(), request.specialty(), request.date());

            LocalDateTime dateTime = LocalDateTime.parse(request.date(), DATE_FORMAT);
            String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            // Serviço: {nome do serviço} - {data} - {tutor e pet}
            String title = String.format("Serviço: %s - %s - %s",
                    request.specialty(), formattedDate, request.patientName());

            String eventLink = calendarService.createAppointment(request.calendarId(), title, dateTime);

            return Map.of("event_link", eventLink, "status", "Agendamento criado com sucesso");
        };
    }
}
