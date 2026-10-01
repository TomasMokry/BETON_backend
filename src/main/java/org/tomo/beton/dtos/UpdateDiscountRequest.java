package org.tomo.beton.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDiscountRequest {
    @NotNull(message = "Discount percent must be provided.")
    private Integer discountPercent;
}
