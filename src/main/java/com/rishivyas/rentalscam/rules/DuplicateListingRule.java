package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.ListingAnalysis;
import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import com.rishivyas.rentalscam.repository.ListingAnalysisRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Cloned listings: the same ad text previously checked with different contact details.
 * Scammers copy genuine ads and swap in their own email or phone.
 */
@Component
public class DuplicateListingRule implements ScamRule {

    private final ListingAnalysisRepository repository;

    public DuplicateListingRule(ListingAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        if (listing.normalizedEmail() == null && listing.normalizedPhone() == null) return List.of();
        // Very short descriptions collide too easily to be meaningful.
        if (listing.request().description().length() < 120) return List.of();

        List<ListingAnalysis> previous = repository.findTop50ByFingerprintOrderByAnalyzedAtDesc(listing.fingerprint());
        long otherContacts = previous.stream()
                .filter(p -> p.getContactEmail() != null || p.getContactPhone() != null)
                .filter(p -> !sameContact(p, listing))
                .map(p -> p.getContactEmail() + "|" + p.getContactPhone())
                .distinct()
                .count();
        if (otherContacts == 0) return List.of();

        return List.of(new Signal("DUPLICATE_CLONED_LISTING", SignalCategory.DUPLICATE, Severity.HIGH, 40,
                "Same ad text seen with different contact details",
                "This description was previously checked with " + otherContacts + " other contact(s).",
                "Search for the ad text or photos online; find the original listing and contact that advertiser instead."));
    }

    private static boolean sameContact(ListingAnalysis previous, ListingContext current) {
        boolean emailMatch = current.normalizedEmail() != null
                && Objects.equals(previous.getContactEmail(), current.normalizedEmail());
        boolean phoneMatch = current.normalizedPhone() != null
                && Objects.equals(previous.getContactPhone(), current.normalizedPhone());
        return emailMatch || phoneMatch;
    }
}
