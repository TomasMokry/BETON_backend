package org.tomo.beton.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.tomo.beton.validations.NotNullEmptyBlank;

import java.time.LocalDate;

@Data
public class MarketPlaceRequest {
    @NotNullEmptyBlank
    @Size(max = 255)
    private String name;
    @Size(max = 500)
    private String address;
    @NotNull(message = "Type is required")
    private MarketPlaceType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private String notes;

    @AssertTrue(message = "End date must not be before start date")
    public boolean isEndDateValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
