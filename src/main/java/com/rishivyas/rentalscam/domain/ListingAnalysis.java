package com.rishivyas.rentalscam.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persisted result of one analysis. The description itself is not stored — only a
 * fingerprint of it, which is enough to spot the same ad re-posted with different contacts.
 */
@Entity
@Table(name = "listing_analysis", indexes = {
        @Index(name = "idx_analysis_fingerprint", columnList = "fingerprint")
})
public class ListingAnalysis {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant analyzedAt;

    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(length = 254)
    private String contactEmail;

    @Column(length = 32)
    private String contactPhone;

    @Column(length = 120)
    private String location;

    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyRent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_analysis_signal", joinColumns = @JoinColumn(name = "analysis_id"))
    @OrderColumn(name = "signal_order")
    private List<StoredSignal> signals = new ArrayList<>();

    protected ListingAnalysis() {
    }

    public ListingAnalysis(String fingerprint, String contactEmail, String contactPhone,
                           String location, BigDecimal monthlyRent, int score, RiskLevel riskLevel,
                           List<Signal> signals) {
        this.id = UUID.randomUUID();
        this.analyzedAt = Instant.now();
        this.fingerprint = fingerprint;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.location = location;
        this.monthlyRent = monthlyRent;
        this.score = score;
        this.riskLevel = riskLevel;
        signals.forEach(s -> this.signals.add(StoredSignal.from(s)));
    }

    public UUID getId() { return id; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public int getScore() { return score; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getFingerprint() { return fingerprint; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public String getLocation() { return location; }
    public BigDecimal getMonthlyRent() { return monthlyRent; }

    public List<Signal> getSignals() {
        return signals.stream().map(StoredSignal::toSignal).toList();
    }
}
