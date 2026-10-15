package com.rishivyas.rentalscam.engine;

import com.rishivyas.rentalscam.domain.Signal;

import java.util.List;

/**
 * One independent check. Rules are Spring beans; adding a new rule is just adding a new
 * {@code @Component} implementing this interface.
 */
public interface ScamRule {

    /** Returns zero or more red flags for the listing. Must not throw on missing optional fields. */
    List<Signal> evaluate(ListingContext listing);
}
