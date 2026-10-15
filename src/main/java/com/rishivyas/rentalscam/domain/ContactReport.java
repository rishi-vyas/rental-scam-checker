package com.rishivyas.rentalscam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A community report that a given email address or phone number was used in a rental scam.
 */
@Entity
@Table(name = "contact_report", indexes = {
        @Index(name = "idx_report_contact", columnList = "contactType, normalizedValue")
})
public class ContactReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContactType contactType;

    @Column(nullable = false, length = 254)
    private String normalizedValue;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant reportedAt;

    protected ContactReport() {
    }

    public ContactReport(ContactType contactType, String normalizedValue, String reason) {
        this.contactType = contactType;
        this.normalizedValue = normalizedValue;
        this.reason = reason;
        this.reportedAt = Instant.now();
    }

    public Long getId() { return id; }
    public ContactType getContactType() { return contactType; }
    public String getNormalizedValue() { return normalizedValue; }
    public String getReason() { return reason; }
    public Instant getReportedAt() { return reportedAt; }
}
