package com.rishivyas.rentalscam.domain;

/**
 * Overall risk band derived from the 0-100 score.
 */
public enum RiskLevel {
    LOW("No strong scam indicators found. Still verify before paying anything."),
    MEDIUM("Some warning signs. Verify the landlord and view the property in person before paying."),
    HIGH("Multiple strong scam indicators. Do not send money until every flag is resolved."),
    CRITICAL("This matches well-known rental scam patterns. Do not pay, and consider reporting it.");

    private final String summary;

    RiskLevel(String summary) {
        this.summary = summary;
    }

    public String summary() {
        return summary;
    }

    public static RiskLevel fromScore(int score) {
        if (score >= 70) return CRITICAL;
        if (score >= 45) return HIGH;
        if (score >= 20) return MEDIUM;
        return LOW;
    }
}
