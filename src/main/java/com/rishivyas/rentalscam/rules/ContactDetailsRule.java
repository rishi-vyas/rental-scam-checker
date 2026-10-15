package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ContactNormalizer;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Weak signals from the contact details themselves. Each is low-weight on its own and only
 * matters in combination with other flags.
 */
@Component
public class ContactDetailsRule implements ScamRule {

    private static final Set<String> FREE_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yahoo.com", "yahoo.ie", "yahoo.co.uk", "hotmail.com", "hotmail.co.uk",
            "outlook.com", "live.com", "aol.com", "icloud.com", "gmx.com", "mail.com", "protonmail.com", "proton.me");

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        List<Signal> signals = new ArrayList<>();
        String email = listing.normalizedEmail();
        String phone = listing.normalizedPhone();

        if (phone != null && !ContactNormalizer.isIrishNumber(phone)) {
            signals.add(new Signal("CONTACT_FOREIGN_PHONE", SignalCategory.CONTACT, Severity.MEDIUM, 15,
                    "Non-Irish phone number for an Irish property",
                    "Phone number " + phone + " is not an Irish (+353) number.",
                    "Ask why a local landlord or agent uses a foreign number, and call it rather than only messaging."));
        }

        if (email != null && phone == null && isFreeEmail(email)) {
            signals.add(new Signal("CONTACT_EMAIL_ONLY", SignalCategory.CONTACT, Severity.LOW, 8,
                    "Only a free webmail address, no phone number",
                    "Contact is " + domainOf(email) + " with no phone number.",
                    "Ask for a phone call; scammers avoid voice contact."));
        }

        if (email != null && emailLooksLikeImpersonation(email)) {
            signals.add(new Signal("CONTACT_IMPERSONATION_EMAIL", SignalCategory.CONTACT, Severity.HIGH, 35,
                    "Email address imitates a platform or company",
                    "Address " + email + " uses a platform name on a free email domain.",
                    "Property portals and Airbnb never email from free webmail addresses."));
        }

        return signals;
    }

    private static boolean isFreeEmail(String email) {
        return FREE_EMAIL_DOMAINS.contains(domainOf(email));
    }

    private static boolean emailLooksLikeImpersonation(String email) {
        if (!isFreeEmail(email)) return false;
        String local = email.substring(0, email.indexOf('@'));
        return local.matches(".*(daft|airbnb|myhome|rent\\.?ie|rtb|booking|support|security|payments?).*");
    }

    private static String domainOf(String email) {
        int at = email.lastIndexOf('@');
        return at < 0 ? "" : email.substring(at + 1);
    }
}
