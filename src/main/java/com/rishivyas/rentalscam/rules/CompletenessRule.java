package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Thin listings with almost no detail. Very weak on its own.
 */
@Component
public class CompletenessRule implements ScamRule {

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        String description = listing.request().description().trim();
        String location = listing.request().location();
        boolean thin = description.length() < 120;
        boolean noLocation = location == null || location.isBlank();
        if (!thin && !noLocation) return List.of();

        String detail = thin && noLocation
                ? "Very short description and no location."
                : thin ? "Very short description (" + description.length() + " characters)." : "No location given.";
        return List.of(new Signal("COMPLETENESS_THIN_LISTING", SignalCategory.COMPLETENESS, Severity.LOW, 6,
                "Listing has very little detail", detail,
                "Ask for the full address and Eircode so you can verify the property exists."));
    }
}
