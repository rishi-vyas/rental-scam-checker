package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.config.CommunityProperties;
import com.rishivyas.rentalscam.domain.ContactType;
import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import com.rishivyas.rentalscam.repository.ContactReportRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Contact details that other users have reported as used in a scam.
 */
@Component
public class CommunityReportRule implements ScamRule {

    private final ContactReportRepository reports;
    private final CommunityProperties properties;

    public CommunityReportRule(ContactReportRepository reports, CommunityProperties properties) {
        this.reports = reports;
        this.properties = properties;
    }

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        List<Signal> signals = new ArrayList<>();
        check(ContactType.EMAIL, listing.normalizedEmail(), signals);
        check(ContactType.PHONE, listing.normalizedPhone(), signals);
        return signals;
    }

    private void check(ContactType type, String value, List<Signal> out) {
        if (value == null) return;
        long count = reports.countByContactTypeAndNormalizedValue(type, value);
        if (count == 0) return;

        boolean confirmed = count >= properties.criticalReportThreshold();
        String label = type == ContactType.EMAIL ? "email address" : "phone number";
        out.add(new Signal("COMMUNITY_REPORTED_" + type.name(), SignalCategory.COMMUNITY,
                confirmed ? Severity.CRITICAL : Severity.HIGH,
                confirmed ? 75 : 40,
                "This " + label + " has been reported for scams",
                value + " has " + count + " community report(s).",
                "Stop contact with this advertiser; the same details have been reported by other renters."));
    }
}
