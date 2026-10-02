package org.tomo.beton.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReopenOrderRequest {
    @NotNull(message = "CartId is required")
    private UUID cartId;
}
