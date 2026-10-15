package com.rishivyas.rentalscam.api.dto;

import com.rishivyas.rentalscam.domain.ContactType;

import java.time.Instant;

public record ContactCheckResponse(
        ContactType type,
        String normalizedValue,
        long reportCount,
        Instant lastReportedAt,
        boolean flagged) {
}
