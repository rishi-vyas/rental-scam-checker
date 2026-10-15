package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.PhraseMatcher;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pressure tactics, requests for identity documents up front, and moving the conversation
 * off the platform.
 */
@Component
public class LanguageRule implements ScamRule {

    private static final PhraseMatcher PRESSURE = PhraseMatcher.of(
            "first come,? first served",
            "(?:lots|loads|a lot) of (?:people )?interest(?:ed)?",
            "many (?:people|others) (?:are )?interested",
            "act (?:fast|quickly|now)",
            "(?:today|tonight) only",
            "within (?:the next )?(?:24|48) hours",
            "(?:will|might) go (?:very )?(?:fast|quickly)",
            "urgent(?:ly)?");

    private static final PhraseMatcher IDENTITY_DOCS = PhraseMatcher.of(
            "(?:send|forward|email) (?:me |us )?(?:a )?(?:copy of |scan of |photo of )?(?:your )?(?:passport|id card|driving licen[cs]e|pps(?: number)?|bank statements?)");

    private static final PhraseMatcher OFF_PLATFORM = PhraseMatcher.of(
            "whats ?app only",
            "(?:contact|reach|email) (?:me )?(?:only )?(?:on|at|via) (?:my )?(?:private|personal) (?:email|e-mail|number)",
            "(?:text|email) only",
            "do not (?:call|use the (?:site|platform) messag(?:e|ing))");

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        List<Signal> signals = new ArrayList<>();
        String text = listing.text();

        List<String> pressure = PRESSURE.find(text);
        if (!pressure.isEmpty()) {
            int points = pressure.size() >= 2 ? 20 : 12;
            signals.add(new Signal("LANGUAGE_PRESSURE", SignalCategory.LANGUAGE,
                    pressure.size() >= 2 ? Severity.MEDIUM : Severity.LOW, points,
                    "Uses pressure or urgency tactics", PhraseMatcher.quote(pressure),
                    "Don't let urgency rush you past verification; genuine landlords allow time for checks."));
        }

        List<String> identity = IDENTITY_DOCS.find(text);
        if (!identity.isEmpty()) {
            signals.add(new Signal("IDENTITY_DOCS_UPFRONT", SignalCategory.IDENTITY, Severity.MEDIUM, 20,
                    "Asks for identity documents up front", PhraseMatcher.quote(identity),
                    "Only share ID or bank statements after a viewing, with a verified agent, and watermark copies."));
        }

        List<String> offPlatform = OFF_PLATFORM.find(text);
        if (!offPlatform.isEmpty()) {
            signals.add(new Signal("CONTACT_OFF_PLATFORM", SignalCategory.CONTACT, Severity.MEDIUM, 12,
                    "Pushes communication off the listing platform", PhraseMatcher.quote(offPlatform),
                    "Keep messages on the listing platform so there is a record and the platform can act on reports."));
        }

        return signals;
    }
}
