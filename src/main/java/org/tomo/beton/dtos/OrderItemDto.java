package org.tomo.beton.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private OrderProductDto product;
    private int quantity;
    private BigDecimal unitPrice;
    private int discountPercent;
    private BigDecimal totalPrice;
}