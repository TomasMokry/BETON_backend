package org.tomo.beton.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDto {
    private CartProductDto product;
    private int quantity;
    private BigDecimal subtotalPrice;
    private int discountPercent;
    private BigDecimal totalPrice;
}
