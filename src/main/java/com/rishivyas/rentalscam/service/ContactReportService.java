package com.rishivyas.rentalscam.service;

import com.rishivyas.rentalscam.api.dto.ContactCheckResponse;
import com.rishivyas.rentalscam.api.dto.ContactReportRequest;
import com.rishivyas.rentalscam.domain.ContactReport;
import com.rishivyas.rentalscam.domain.ContactType;
import com.rishivyas.rentalscam.engine.ContactNormalizer;
import com.rishivyas.rentalscam.repository.ContactReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ContactReportService {

    private final ContactReportRepository repository;

    public ContactReportService(ContactReportRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ContactCheckResponse report(ContactReportRequest request) {
        String normalized = normalizeOrThrow(request.type(), request.value());
        repository.save(new ContactReport(request.type(), normalized, request.reason()));
        return check(request.type(), request.value());
    }

    @Transactional(readOnly = true)
    public ContactCheckResponse check(ContactType type, String value) {
        String normalized = normalizeOrThrow(type, value);
        long count = repository.countByContactTypeAndNormalizedValue(type, normalized);
        Instant last = repository.findFirstByContactTypeAndNormalizedValueOrderByReportedAtDesc(type, normalized)
                .map(ContactReport::getReportedAt)
                .orElse(null);
        return new ContactCheckResponse(type, normalized, count, last, count > 0);
    }

    private static String normalizeOrThrow(ContactType type, String value) {
        String normalized = ContactNormalizer.normalize(type, value);
        if (normalized == null) {
            throw new IllegalArgumentException("Invalid " + type.name().toLowerCase() + ": " + value);
        }
        if (type == ContactType.EMAIL && !normalized.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        if (type == ContactType.PHONE && normalized.length() < 8) {
            throw new IllegalArgumentException("Invalid phone number: " + value);
        }
        return normalized;
    }
}
