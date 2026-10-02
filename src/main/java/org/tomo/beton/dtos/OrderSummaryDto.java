package org.tomo.beton.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderSummaryDto {
    private Long marketPlaceId;
    private String marketPlaceName;
    private long orderCount;
    private BigDecimal cardTotal;
    private BigDecimal cashTotal;
    private BigDecimal total;
}
