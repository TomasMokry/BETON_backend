package org.tomo.beton.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CartDto {
    private UUID id;
    private BigDecimal subtotalPrice = BigDecimal.ZERO;
    private int discountPercent;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal totalPrice = BigDecimal.ZERO;
    private List<CartItemDto> items = new ArrayList<>();
}
