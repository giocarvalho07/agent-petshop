package com.mentoriatech.agent.dto;

public record DoctorResponse(
        Long id,
        String name,
        String specialty,
        String calendarId,
        String message
) {}
