package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.TestListings;
import com.rishivyas.rentalscam.api.dto.ListingRequest;
import com.rishivyas.rentalscam.config.RentBenchmarkProperties;
import com.rishivyas.rentalscam.domain.PaymentMethod;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.engine.ListingContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RulesTest {

    private static List<String> codes(List<Signal> signals) {
        return signals.stream().map(Signal::code).toList();
    }

    private static ListingContext ctx(ListingRequest r) {
        return new ListingContext(r);
    }

    private static RentBenchmarkProperties benchmarks() {
        LinkedHashMap<String, BigDecimal> medians = new LinkedHashMap<>();
        medians.put("dublin 4", new BigDecimal("2300"));
        medians.put("dublin", new BigDecimal("1900"));
        return new RentBenchmarkProperties(new BigDecimal("1300"), medians,
                Map.of(0, new BigDecimal("0.8"), 1, BigDecimal.ONE, 2, new BigDecimal("1.3"), 3, new BigDecimal("1.6")),
                new BigDecimal("0.75"), new BigDecimal("0.55"));
    }

    @Test
    void priceRuleFlagsFarBelowMarketAndUsesMostSpecificArea() {
        PriceAnomalyRule rule = new PriceAnomalyRule(benchmarks());
        // Dublin 4, 2-bed typical = 2300 * 1.3 = 2990; 900 is ~30%.
        assertThat(codes(rule.evaluate(ctx(TestListings.scam())))).containsExactly("PRICE_FAR_BELOW_MARKET");
        assertThat(rule.typicalRent("Ballsbridge, Dublin 4", 2)).isEqualByComparingTo("2990");
        assertThat(rule.typicalRent("Dublin 22", 1)).isEqualByComparingTo("1900");
        assertThat(rule.typicalRent("Sligo", 3)).isEqualByComparingTo("2080");
    }

    @Test
    void priceRuleIgnoresMarketRateListings() {
        PriceAnomalyRule rule = new PriceAnomalyRule(benchmarks());
        assertThat(rule.evaluate(ctx(TestListings.genuine()))).isEmpty();
    }

    @Test
    void paymentRuleFlagsUntraceableMethodsAndPayBeforeViewing() {
        List<String> codes = codes(new PaymentRule().evaluate(ctx(TestListings.scam())));
        assertThat(codes).contains("PAYMENT_UNTRACEABLE_METHOD", "PAYMENT_BEFORE_VIEWING");
    }

    @Test
    void paymentRuleDetectsMethodsMentionedOnlyInText() {
        List<Signal> signals = new PaymentRule().evaluate(ctx(TestListings.minimal(
                "Room available. Rent can be paid in bitcoin or Steam cards.")));
        assertThat(codes(signals)).containsExactly("PAYMENT_UNTRACEABLE_METHOD");
        assertThat(signals.get(0).detail()).contains("bitcoin", "steam cards");
    }

    @Test
    void paymentRuleFlagsPaypalFriendsAndFamily() {
        ListingRequest r = new ListingRequest(null, "Nice room", null, null, null, null, null, null, null,
                List.of(PaymentMethod.PAYPAL_FRIENDS_AND_FAMILY), null, null);
        assertThat(codes(new PaymentRule().evaluate(ctx(r)))).containsExactly("PAYMENT_NO_BUYER_PROTECTION");
    }

    @Test
    void viewingRuleFlagsAbsentLandlordAndFakeEscrow() {
        List<String> codes = codes(new ViewingRule().evaluate(ctx(TestListings.scam())));
        assertThat(codes).containsExactlyInAnyOrder("VIEWING_ABSENT_LANDLORD", "VIEWING_FAKE_THIRD_PARTY");
    }

    @Test
    void viewingRuleFlagsRefusedViewingWhenNoStoryGiven() {
        ListingRequest r = new ListingRequest(null, "Apartment for rent", null, null, null, null, null, null, null,
                null, false, null);
        assertThat(codes(new ViewingRule().evaluate(ctx(r)))).containsExactly("VIEWING_REFUSED");
    }

    @Test
    void depositRuleUsesOneMonthCap() {
        assertThat(codes(new DepositRule().evaluate(ctx(TestListings.scam())))).containsExactly("DEPOSIT_EXCESSIVE");
        assertThat(new DepositRule().evaluate(ctx(TestListings.genuine()))).isEmpty();
    }

    @Test
    void languageRuleFlagsPressureAndIdentityRequests() {
        List<String> codes = codes(new LanguageRule().evaluate(ctx(TestListings.scam())));
        assertThat(codes).contains("LANGUAGE_PRESSURE", "IDENTITY_DOCS_UPFRONT");
    }

    @Test
    void contactRuleFlagsForeignPhoneAndImpersonationEmail() {
        List<String> codes = codes(new ContactDetailsRule().evaluate(ctx(TestListings.scam())));
        assertThat(codes).containsExactlyInAnyOrder("CONTACT_FOREIGN_PHONE", "CONTACT_IMPERSONATION_EMAIL");
        assertThat(new ContactDetailsRule().evaluate(ctx(TestListings.genuine()))).isEmpty();
    }

    @Test
    void linkRuleFlagsLookalikeButNotRealDomains() {
        SuspiciousLinkRule rule = new SuspiciousLinkRule();
        assertThat(codes(rule.evaluate(ctx(TestListings.scam())))).containsExactly("LINK_LOOKALIKE_DOMAIN");
        assertThat(rule.evaluate(ctx(TestListings.genuine()))).isEmpty();
        assertThat(SuspiciousLinkRule.mentionsBrand("artbooks.com", "rtb")).isFalse();
        assertThat(SuspiciousLinkRule.mentionsBrand("daftsecure.net", "daft")).isTrue();
        assertThat(SuspiciousLinkRule.mentionsBrand("secure-airbnb.co", "airbnb")).isTrue();
    }

    @Test
    void genuineListingTriggersNoStrongSignals() {
        ListingContext genuine = ctx(TestListings.genuine());
        assertThat(new PaymentRule().evaluate(genuine)).isEmpty();
        assertThat(new ViewingRule().evaluate(genuine)).isEmpty();
        assertThat(new LanguageRule().evaluate(genuine)).isEmpty();
        assertThat(new CompletenessRule().evaluate(genuine)).isEmpty();
    }
}
