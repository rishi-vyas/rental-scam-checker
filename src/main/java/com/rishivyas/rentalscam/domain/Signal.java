package com.rishivyas.rentalscam.domain;

/**
 * A single red flag raised by a rule.
 *
 * @param code     stable machine-readable identifier, e.g. {@code PAYMENT_UNTRACEABLE_METHOD}
 * @param category grouping used for reporting
 * @param severity how strongly this indicates a scam on its own
 * @param points   contribution to the score (0-100), combined with other signals as independent evidence
 * @param title    short human-readable label
 * @param detail   the evidence that triggered it (matched phrase, computed ratio, ...)
 * @param advice   what the tenant should do about it
 */
public record Signal(
        String code,
        SignalCategory category,
        Severity severity,
        int points,
        String title,
        String detail,
        String advice) {

    public Signal {
        if (points < 0 || points > 100) {
            throw new IllegalArgumentException("points must be between 0 and 100");
        }
    }
}
