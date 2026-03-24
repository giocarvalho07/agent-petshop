package com.mentoriatech.agent.dto;

public record VetResponse(
        Long id,
        String name,
        String specialty,
        String calendarId,
        String message
) {}
