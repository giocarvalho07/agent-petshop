package com.mentoriatech.agent.dto;

public record AppointmentRequest(String calendarId, String patientName, String specialty, String date) {}
