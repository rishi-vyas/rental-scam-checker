package com.rishivyas.rentalscam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Typical monthly rents used by the price-anomaly rule.
 *
 * <p>{@code oneBedMedians} maps a lowercase location keyword to a typical 1-bed rent. The first
 * key contained in the listing's location wins, so list more specific keys (e.g. "dublin 4")
 * before broader ones ("dublin"). {@code bedroomMultipliers} scales that figure by size.
 *
 * <p>Defaults in application.yml are illustrative placeholders — replace them with current
 * figures (e.g. from the RTB Rent Index) before relying on this rule.
 */
@ConfigurationProperties(prefix = "scam-checker.rent-benchmarks")
public record RentBenchmarkProperties(
        BigDecimal defaultOneBedMedian,
        LinkedHashMap<String, BigDecimal> oneBedMedians,
        Map<Integer, BigDecimal> bedroomMultipliers,
        BigDecimal suspiciousRatio,
        BigDecimal severeRatio) {

    public RentBenchmarkProperties {
        if (oneBedMedians == null) oneBedMedians = new LinkedHashMap<>();
        if (bedroomMultipliers == null) bedroomMultipliers = Map.of();
        if (suspiciousRatio == null) suspiciousRatio = new BigDecimal("0.75");
        if (severeRatio == null) severeRatio = new BigDecimal("0.55");
    }
}
