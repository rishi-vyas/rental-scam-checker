package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.PaymentMethod;
import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.PhraseMatcher;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Untraceable or non-refundable payment methods, and money requested before a viewing.
 */
@Component
public class PaymentRule implements ScamRule {

    private static final Set<PaymentMethod> UNTRACEABLE = EnumSet.of(
            PaymentMethod.WESTERN_UNION, PaymentMethod.MONEYGRAM, PaymentMethod.CRYPTO, PaymentMethod.GIFT_CARD);

    private static final PhraseMatcher UNTRACEABLE_TEXT = PhraseMatcher.of(
            "western union", "money ?gram", "bitcoin", "crypto(?:currency)?", "usdt", "tether",
            "gift ?cards?", "steam cards?", "amazon (?:gift )?vouchers?");

    private static final PhraseMatcher PAY_FIRST_TEXT = PhraseMatcher.of(
            "pay(?:ment)? (?:the )?(?:deposit )?before (?:the )?viewing",
            "deposit (?:first|upfront|up front) (?:and|then) (?:i|we)(?:'ll| will) (?:send|post|courier)",
            "(?:send|transfer|pay) (?:the )?(?:deposit|first month(?:'s)? rent) (?:to|and) (?:secure|reserve|hold)",
            "refundable (?:holding )?deposit to (?:arrange|book) (?:a )?viewing");

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        List<Signal> signals = new ArrayList<>();

        Set<String> untraceable = new LinkedHashSet<>();
        listing.request().paymentMethodsOrEmpty().stream()
                .filter(UNTRACEABLE::contains)
                .map(m -> m.name().toLowerCase().replace('_', ' '))
                .forEach(untraceable::add);
        untraceable.addAll(UNTRACEABLE_TEXT.find(listing.text()));
        if (!untraceable.isEmpty()) {
            signals.add(new Signal("PAYMENT_UNTRACEABLE_METHOD", SignalCategory.PAYMENT, Severity.CRITICAL, 60,
                    "Asks for an untraceable payment method",
                    "Payment via: " + String.join(", ", untraceable),
                    "Never pay rent or a deposit by money transfer service, crypto or gift cards — the money cannot be recovered."));
        }

        if (listing.request().paymentMethodsOrEmpty().contains(PaymentMethod.PAYPAL_FRIENDS_AND_FAMILY)) {
            signals.add(new Signal("PAYMENT_NO_BUYER_PROTECTION", SignalCategory.PAYMENT, Severity.HIGH, 30,
                    "Asks for PayPal 'Friends and Family'",
                    "Friends and Family payments are not covered by buyer protection.",
                    "Refuse payment methods that remove buyer protection."));
        }

        if (listing.request().paymentMethodsOrEmpty().contains(PaymentMethod.INTERNATIONAL_WIRE)) {
            signals.add(new Signal("PAYMENT_INTERNATIONAL_WIRE", SignalCategory.PAYMENT, Severity.MEDIUM, 15,
                    "Asks for an international bank transfer",
                    "Rent for an Irish property being sent to a foreign account.",
                    "Ask why rent for a local property must go to an overseas account."));
        }

        List<String> payFirstHits = PAY_FIRST_TEXT.find(listing.text());
        if (Boolean.TRUE.equals(listing.request().paymentRequestedBeforeViewing()) || !payFirstHits.isEmpty()) {
            String detail = payFirstHits.isEmpty()
                    ? "Money was requested before you viewed the property."
                    : PhraseMatcher.quote(payFirstHits);
            signals.add(new Signal("PAYMENT_BEFORE_VIEWING", SignalCategory.PAYMENT, Severity.HIGH, 45,
                    "Payment requested before a viewing", detail,
                    "Do not pay anything until you have viewed the property and met the landlord or agent."));
        }

        return signals;
    }
}
