package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.TestListings;
import com.rishivyas.rentalscam.domain.ListingAnalysis;
import com.rishivyas.rentalscam.domain.RiskLevel;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.repository.ListingAnalysisRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicateListingRuleTest {

    private final ListingAnalysisRepository repository = mock(ListingAnalysisRepository.class);
    private final DuplicateListingRule rule = new DuplicateListingRule(repository);

    private static ListingAnalysis previous(String email, String phone) {
        return new ListingAnalysis("fp", email, phone, null, null, 0, RiskLevel.LOW, List.of());
    }

    @Test
    void flagsSameTextWithDifferentContact() {
        when(repository.findTop50ByFingerprintOrderByAnalyzedAtDesc(anyString()))
                .thenReturn(List.of(previous("real.landlord@example.ie", "+353871112222")));

        assertThat(rule.evaluate(new ListingContext(TestListings.genuine())))
                .extracting("code").containsExactly("DUPLICATE_CLONED_LISTING");
    }

    @Test
    void ignoresRepostBySameAdvertiser() {
        ListingContext ctx = new ListingContext(TestListings.genuine());
        when(repository.findTop50ByFingerprintOrderByAnalyzedAtDesc(anyString()))
                .thenReturn(List.of(previous(ctx.normalizedEmail(), null)));

        assertThat(rule.evaluate(ctx)).isEmpty();
    }
}
