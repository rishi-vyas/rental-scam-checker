package com.rishivyas.rentalscam.rules;

import com.rishivyas.rentalscam.domain.Severity;
import com.rishivyas.rentalscam.domain.Signal;
import com.rishivyas.rentalscam.domain.SignalCategory;
import com.rishivyas.rentalscam.engine.ListingContext;
import com.rishivyas.rentalscam.engine.ScamRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Deposit larger than one month's rent. In Ireland, deposits for most residential tenancies
 * are capped at one month's rent, so a bigger ask is either non-compliant or a grab for cash.
 */
@Component
public class DepositRule implements ScamRule {

    private static final BigDecimal TOLERANCE = new BigDecimal("1.05");

    @Override
    public List<Signal> evaluate(ListingContext listing) {
        BigDecimal rent = listing.request().monthlyRent();
        BigDecimal deposit = listing.request().depositAmount();
        if (rent == null || deposit == null || rent.signum() <= 0) return List.of();

        BigDecimal months = deposit.divide(rent, 2, RoundingMode.HALF_UP);
        String detail = String.format(Locale.ROOT, "Deposit of €%s is %s× the monthly rent.",
                deposit.setScale(0, RoundingMode.HALF_UP).toPlainString(), months.toPlainString());

        if (months.compareTo(new BigDecimal("2")) > 0) {
            return List.of(new Signal("DEPOSIT_EXCESSIVE", SignalCategory.DEPOSIT, Severity.HIGH, 30,
                    "Deposit is far above one month's rent", detail,
                    "Deposits for most Irish tenancies are capped at one month's rent; ask the RTB if unsure."));
        }
        if (months.compareTo(TOLERANCE) > 0) {
            return List.of(new Signal("DEPOSIT_ABOVE_LIMIT", SignalCategory.DEPOSIT, Severity.MEDIUM, 15,
                    "Deposit is above one month's rent", detail,
                    "Deposits for most Irish tenancies are capped at one month's rent; ask the RTB if unsure."));
        }
        return List.of();
    }
}
