package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.config.RentBenchmarkProperties;
import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Too good to be true" pricing: rent far below what similar homes in the area typically cost.
 */
@Component
public class PriceAnomalyRule implements ScamRule {

    private final RentBenchmarkProperties benchmarks;

    public PriceAnomalyRule(RentBenchmarkProperties benchmarks) {
        this.benchmarks = benchmarks;
    }

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        BigDecimal rent = listing.request().monthlyRent();
        if (rent == null) return List.of();

        BigDecimal typical = typicalRent(listing.request().location(), listing.request().bedrooms());
        if (typical == null || typical.signum() <= 0) return List.of();

        BigDecimal ratio = rent.divide(typical, 3, RoundingMode.HALF_UP);
        String detail = String.format(Locale.ROOT,
                "Asking €%s/month is %d%% of a typical €%s for this size and area.",
                rent.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                ratio.multiply(BigDecimal.valueOf(100)).intValue(),
                typical.setScale(0, RoundingMode.HALF_UP).toPlainString());

        if (ratio.compareTo(benchmarks.severeRatio()) < 0) {
            return List.of(new Signal("PRICE_FAR_BELOW_MARKET", SignalCategory.PRICE, Severity.HIGH, 40,
                    "Rent is far below the local market", detail,
                    "Treat a rent this far below market as a lure until proven otherwise; ask why it is so cheap."));
        }
        if (ratio.compareTo(benchmarks.suspiciousRatio()) < 0) {
            return List.of(new Signal("PRICE_BELOW_MARKET", SignalCategory.PRICE, Severity.MEDIUM, 18,
                    "Rent is noticeably below the local market", detail,
                    "Compare with similar listings nearby; unusually cheap rent is the most common scam lure."));
        }
        return List.of();
    }

    BigDecimal typicalRent(String location, Integer bedrooms) {
        BigDecimal oneBed = benchmarks.defaultOneBedMedian();
        if (location != null) {
            String loc = location.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, BigDecimal> e : benchmarks.oneBedMedians().entrySet()) {
                if (containsArea(loc, e.getKey().toLowerCase(Locale.ROOT))) {
                    oneBed = e.getValue();
                    break;
                }
            }
        }
        if (oneBed == null) return null;
        int beds = bedrooms == null ? 1 : bedrooms;
        BigDecimal multiplier = multiplierFor(beds);
        return oneBed.multiply(multiplier);
    }

    /** Word-boundary match so "dublin 2" does not match "dublin 22". */
    private static boolean containsArea(String location, String key) {
        int idx = location.indexOf(key);
        while (idx >= 0) {
            int end = idx + key.length();
            boolean startOk = idx == 0 || !Character.isLetterOrDigit(location.charAt(idx - 1));
            boolean endOk = end == location.length() || !Character.isLetterOrDigit(location.charAt(end));
            if (startOk && endOk) return true;
            idx = location.indexOf(key, idx + 1);
        }
        return false;
    }

    private BigDecimal multiplierFor(int beds) {
        Map<Integer, BigDecimal> m = benchmarks.bedroomMultipliers();
        if (m.containsKey(beds)) return m.get(beds);
        // Beyond the configured sizes, use the largest configured multiplier.
        return m.entrySet().stream()
                .filter(e -> e.getKey() <= beds)
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(BigDecimal.ONE);
    }
}
