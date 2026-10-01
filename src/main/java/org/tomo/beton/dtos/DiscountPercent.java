package org.tomo.beton.dtos;

import org.tomo.beton.excetions.InvalidDiscountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public final class DiscountPercent {
    public static final Set<Integer> ALLOWED = Set.of(0, 5, 10, 15, 20, 25, 30, 100);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private DiscountPercent() {
    }

    public static void validate(Integer percent) {
        if (percent == null || !ALLOWED.contains(percent)) {
            throw new InvalidDiscountException();
        }
    }

    /** Returns the amount after applying the discount, rounded to 2 decimal places. */
    public static BigDecimal apply(BigDecimal amount, int percent) {
        return amount.subtract(discountOf(amount, percent)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Returns the discount amount, rounded to 2 decimal places. */
    public static BigDecimal discountOf(BigDecimal amount, int percent) {
        return amount.multiply(BigDecimal.valueOf(percent))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
