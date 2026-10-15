package com.rishivyas.rentalscam.api.dto;

import com.rishivyas.rentalscam.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * A rental listing (and what the "landlord" has asked for) to be checked.
 * Only {@code description} is required; every extra field lets more rules run.
 */
public record ListingRequest(
        @Size(max = 300) String title,

        @NotBlank @Size(max = 20_000) String description,

        @DecimalMin(value = "0.0", inclusive = false) BigDecimal monthlyRent,

        /* Free text, e.g. "Dublin 8", "Rathmines, Dublin", "Cork city". */
        @Size(max = 120) String location,

        /* 0 = studio. */
        @Min(0) @Max(10) Integer bedrooms,

        @DecimalMin(value = "0.0") BigDecimal depositAmount,

        @Email @Size(max = 254) String contactEmail,

        @Size(max = 32) String contactPhone,

        @Size(max = 2048) String listingUrl,

        List<PaymentMethod> requestedPaymentMethods,

        /* Has the advertiser agreed to an in-person viewing? null = unknown. */
        Boolean viewingOffered,

        /* Has money been requested before you have viewed the property or signed anything? */
        Boolean paymentRequestedBeforeViewing) {

    public List<PaymentMethod> paymentMethodsOrEmpty() {
        return requestedPaymentMethods == null ? List.of() : requestedPaymentMethods;
    }
}
