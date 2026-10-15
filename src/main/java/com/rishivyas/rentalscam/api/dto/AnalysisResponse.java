package com.rishivyas.rentalscam.api.dto;

import com.rishivyas.rentalscam.domain.ListingAnalysis;
import com.rishivyas.rentalscam.domain.RiskLevel;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.engine.Recommendations;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(
        UUID id,
        Instant analyzedAt,
        int score,
        RiskLevel riskLevel,
        String summary,
        List<Signal> signals,
        List<String> recommendations,
        String disclaimer) {

    private static final String DISCLAIMER =
            "Automated, rule-based assessment. A low score is not a guarantee a listing is genuine. "
                    + "Never pay a deposit before viewing the property in person and confirming who owns it.";

    public static AnalysisResponse from(ListingAnalysis analysis) {
        List<Signal> signals = analysis.getSignals();
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getAnalyzedAt(),
                analysis.getScore(),
                analysis.getRiskLevel(),
                analysis.getRiskLevel().summary(),
                signals,
                Recommendations.build(signals),
                DISCLAIMER);
    }
}
