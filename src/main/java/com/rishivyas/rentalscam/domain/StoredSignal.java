package com.rishivyas.rentalscam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class StoredSignal {

    @Column(nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SignalCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    private int points;

    @Column(length = 200)
    private String title;

    @Column(length = 1000)
    private String detail;

    @Column(length = 1000)
    private String advice;

    protected StoredSignal() {
    }

    public static StoredSignal from(Signal s) {
        StoredSignal stored = new StoredSignal();
        stored.code = s.code();
        stored.category = s.category();
        stored.severity = s.severity();
        stored.points = s.points();
        stored.title = s.title();
        stored.detail = truncate(s.detail(), 1000);
        stored.advice = truncate(s.advice(), 1000);
        return stored;
    }

    public Signal toSignal() {
        return new Signal(code, category, severity, points, title, detail, advice);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
