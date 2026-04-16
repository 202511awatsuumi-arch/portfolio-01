package com.example.demo.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

@Component
public class PreferredDateNormalizer {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter US_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public LocalDate parsePreferredDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String value = rawValue.trim();

        try {
            return LocalDate.parse(value, ISO_FORMAT);
        } catch (DateTimeParseException ignored) {
            // Try the US-style format used by the existing English page.
        }

        try {
            return LocalDate.parse(value, US_FORMAT);
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException("Unsupported preferredDate format: " + rawValue);
        }
    }

    public String normalizePreferredDate(String rawValue) {
        LocalDate parsed = parsePreferredDate(rawValue);
        return parsed == null ? null : parsed.format(ISO_FORMAT);
    }
}
