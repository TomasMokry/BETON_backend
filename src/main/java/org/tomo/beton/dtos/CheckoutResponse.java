package org.tomo.beton.dtos;

import lombok.Data;

@Data
public class CheckoutResponse {
    private Long orderId;

    public CheckoutResponse(Long orderId) {
        this.orderId = orderId;
    }
}
