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
 * The classic "absent landlord": abroad, can't show the place, will post the keys,
 * or routes the deposit through a fake "secure" third party.
 */
@Component
public class ViewingRule implements ScamRule {

    private static final PhraseMatcher ABSENT_LANDLORD = PhraseMatcher.of(
            "(?:i am|i'm|we are|we're) (?:currently |presently |now )?(?:living |working |based )?(?:abroad|overseas|out of (?:the )?country|outside ireland)",
            "(?:moved|relocated) (?:abroad|overseas|to (?:the )?(?:uk|us|usa|france|spain|germany|dubai|canada))",
            "keys? (?:will be |can be )?(?:sent|posted|mailed|couriered|delivered) (?:to you)?",
            "(?:can ?not|can't|cannot|unable to) (?:show|do a viewing of) (?:you )?(?:the )?(?:property|apartment|flat|house|room)",
            "no viewings?(?: (?:possible|available))?",
            "missionary|oil rig|offshore (?:work|job)|on (?:a )?(?:mission|deployment)");

    private static final PhraseMatcher FAKE_ESCROW = PhraseMatcher.of(
            "(?:through|via|by) airbnb",
            "airbnb (?:will )?(?:hold|handle|process|manage) (?:the )?(?:payment|deposit|money)",
            "booking\\.com (?:will )?(?:hold|handle|process)",
            "escrow",
            "(?:secure|safe|protected) (?:payment|deposit|rental) (?:service|platform|system|agent)",
            "daft (?:secure|safe|payment|protection)");

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        List<Signal> signals = new ArrayList<>();

        List<String> absent = ABSENT_LANDLORD.find(listing.text());
        if (!absent.isEmpty()) {
            signals.add(new Signal("VIEWING_ABSENT_LANDLORD", SignalCategory.VIEWING, Severity.HIGH, 45,
                    "Landlord says they are away and can't show the property",
                    PhraseMatcher.quote(absent),
                    "A landlord who is abroad and offers to post the keys is the single most common rental scam story."));
        }

        if (Boolean.FALSE.equals(listing.request().viewingOffered()) && absent.isEmpty()) {
            signals.add(new Signal("VIEWING_REFUSED", SignalCategory.VIEWING, Severity.HIGH, 35,
                    "No in-person viewing offered",
                    "The advertiser has not agreed to a viewing.",
                    "Insist on an in-person viewing; if they refuse, walk away."));
        }

        List<String> escrow = FAKE_ESCROW.find(listing.text());
        if (!escrow.isEmpty()) {
            signals.add(new Signal("VIEWING_FAKE_THIRD_PARTY", SignalCategory.VIEWING, Severity.HIGH, 45,
                    "Payment routed through a 'secure' third party",
                    PhraseMatcher.quote(escrow),
                    "Airbnb, Booking.com and property portals do not hold deposits for long-term rentals; emails claiming they do are fake."));
        }

        return signals;
    }
}
