package com.rishivyas.rentalscam.engine;

import com.rishivyas.rentalscam.domain.Signal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Recommendations {

    private static final List<String> BASELINE = List.of(
            "View the property in person (or via a trusted person) before paying anything.",
            "Confirm the landlord's identity and that they own or manage the property.",
            "Pay only by a traceable method into an account in the landlord's or agent's name, and get a receipt.",
            "Check that the landlord registers the tenancy with the Residential Tenancies Board (RTB).");

    private Recommendations() {
    }

    /** Signal-specific advice first (highest-impact signals first), then baseline checks. */
    public static List<String> build(List<Signal> signals) {
        Set<String> out = new LinkedHashSet<>();
        signals.stream()
                .sorted((a, b) -> Integer.compare(b.points(), a.points()))
                .map(Signal::advice)
                .filter(a -> a != null && !a.isBlank())
                .forEach(out::add);
        out.addAll(BASELINE);
        return List.copyOf(out);
    }
}
