package com.mentoriatech.agent.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@Service
public class GoogleCalendarService {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String APPLICATION_NAME = "Medical Agent";

    private Calendar getCalendarService() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList(CalendarScopes.CALENDAR));

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Verifica se determinada data está disponível no calendário de um médico.
     * Lógica de agenda.
     */
    public boolean checkAvailability(String calendarId, LocalDateTime requestedDate) {
        try {
            Calendar service = getCalendarService();

            ZonedDateTime startZoned = requestedDate.atZone(SAO_PAULO);
            ZonedDateTime endZoned = startZoned.plusHours(1);

            String timeMin = startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String timeMax = endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // Força o uso do calendário principal do usuário logado para testes locais
            Events events = service.events().list("primary")
                    .setTimeMin(new DateTime(timeMin))
                    .setTimeMax(new DateTime(timeMax))
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();

            boolean available = events.getItems().isEmpty();
            log.debug("checkAvailability calendarId={} date={} available={}", calendarId, requestedDate, available);
            return available;

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                log.warn("Calendar not found (404), assuming available: {}", calendarId);
                return true;
            }
            log.error("Error checking availability", e);
            throw new RuntimeException("Erro ao verificar disponibilidade no calendário", e);
        }
    }

    /**
     * Cria um novo evento de consulta no calendário do médico.
     * Lógica de agenda.
     */
    public String createAppointment(String calendarId, String title, LocalDateTime requestedDate) {
        try {
            Calendar service = getCalendarService();

            ZonedDateTime startZoned = requestedDate.atZone(SAO_PAULO);
            ZonedDateTime endZoned = startZoned.plusHours(1);

            Event event = new Event()
                    .setSummary(title)
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                            .setTimeZone(SAO_PAULO.getId()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                            .setTimeZone(SAO_PAULO.getId()));

            Event createdEvent = service.events().insert("primary", event).execute();
            log.info("Appointment created: {} at {} in primary calendar", title, requestedDate);
            return createdEvent.getHtmlLink();

        } catch (Exception e) {
            log.error("Error creating appointment", e);
            throw new RuntimeException("Erro ao criar agendamento no calendário", e);
        }
    }
}
