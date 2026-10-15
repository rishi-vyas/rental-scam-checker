package com.rishivyas.rentalscam.engine;

import com.rishivyas.rentalscam.domain.RiskLevel;
import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScorerTest {

    private static Signal signal(int points) {
        return new Signal("T", SignalCategory.LANGUAGE, Severity.LOW, points, "t", "d", "a");
    }

    @Test
    void noSignalsScoresZero() {
        assertThat(RiskScorer.score(List.of())).isZero();
        assertThat(RiskLevel.fromScore(0)).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void singleSignalScoresItsPoints() {
        assertThat(RiskScorer.score(List.of(signal(40)))).isEqualTo(40);
    }

    @Test
    void signalsCombineAsIndependentEvidenceAndNeverExceed100() {
        // 1 - (0.5 * 0.5) = 0.75
        assertThat(RiskScorer.score(List.of(signal(50), signal(50)))).isEqualTo(75);
        assertThat(RiskScorer.score(List.of(signal(90), signal(90), signal(90), signal(90)))).isLessThanOrEqualTo(100);
    }

    @Test
    void riskBands() {
        assertThat(RiskLevel.fromScore(19)).isEqualTo(RiskLevel.LOW);
        assertThat(RiskLevel.fromScore(20)).isEqualTo(RiskLevel.MEDIUM);
        assertThat(RiskLevel.fromScore(45)).isEqualTo(RiskLevel.HIGH);
        assertThat(RiskLevel.fromScore(70)).isEqualTo(RiskLevel.CRITICAL);
    }
}
