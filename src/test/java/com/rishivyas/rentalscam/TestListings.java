package com.rishivyas.rentalscam;

import com.rishivyas.rentalscam.api.dto.ListingRequest;
import com.rishivyas.rentalscam.domain.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

/** Shared sample listings for tests. */
public final class TestListings {

    private TestListings() {
    }

    public static final String GENUINE_DESCRIPTION = """
            Bright two-bedroom apartment on the second floor of a well-managed block in Rathmines.
            Fully furnished, gas central heating, secure bike storage and a small balcony facing south.
            Viewings on Tuesday and Thursday evenings with the letting agent; please bring references.
            Tenancy will be registered with the RTB. BER C1.""";

    public static final String SCAM_DESCRIPTION = """
            Lovely 2 bed apartment in Dublin 4, fully furnished, all bills included.
            I am currently working abroad on an oil rig so I can't show you the property, but the keys will be
            sent to you by courier once payment is complete. The payment will be handled through Airbnb who will
            hold the deposit for you. Lots of interest so first come first served. Please send a copy of your passport.
            Pay with Western Union. More details at https://daft-ie-secure-rentals.com/listing/8812""";

    public static ListingRequest genuine() {
        return new ListingRequest("2 bed apartment, Rathmines", GENUINE_DESCRIPTION,
                new BigDecimal("2450"), "Rathmines, Dublin 6", 2, new BigDecimal("2450"),
                "lettings@example-agency.ie", "01 555 0123", "https://www.daft.ie/for-rent/12345",
                List.of(PaymentMethod.BANK_TRANSFER), true, false);
    }

    public static ListingRequest scam() {
        return new ListingRequest("Cheap 2 bed Ballsbridge", SCAM_DESCRIPTION,
                new BigDecimal("900"), "Ballsbridge, Dublin 4", 2, new BigDecimal("2700"),
                "daft.support.payments@gmail.com", "+44 7700 900123", null,
                List.of(PaymentMethod.WESTERN_UNION), false, true);
    }

    public static ListingRequest minimal(String description) {
        return new ListingRequest(null, description, null, null, null, null, null, null, null, null, null, null);
    }
}
