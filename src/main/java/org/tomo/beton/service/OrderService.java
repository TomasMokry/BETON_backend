package org.tomo.beton.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tomo.beton.dtos.OrderDto;
import org.tomo.beton.dtos.OrderSummaryDto;
import org.tomo.beton.dtos.PaymentMethod;
import org.tomo.beton.excetions.OrderAccessDeniedException;
import org.tomo.beton.excetions.OrderNotFoundException;
import org.tomo.beton.mappers.OrderMapper;
import org.tomo.beton.repositories.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Service
public class OrderService {
    /** Value of the marketPlaceId filter that selects orders sold outside any market place. */
    public static final String NO_MARKET_PLACE = "none";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AuthService authService;

    public List<OrderDto> getAllOrders(String marketPlaceId) {
        var user = authService.getCurrentUser();

        var orders = marketPlaceId == null || marketPlaceId.isBlank()
                ? orderRepository.getOrdersByCustomer(user)
                : NO_MARKET_PLACE.equals(marketPlaceId)
                    ? orderRepository.getOrdersByCustomerWithoutMarketPlace(user)
                    : orderRepository.getOrdersByCustomerAndMarketPlace(user, Long.valueOf(marketPlaceId));
        return orders.stream().map(orderMapper::toDto).toList();
    }

    public List<OrderSummaryDto> getSummary(String marketPlaceId) {
        var user = authService.getCurrentUser();

        var summaries = new LinkedHashMap<Long, OrderSummaryDto>();
        for (var row : orderRepository.summarizeByMarketPlace(user)) {
            var id = (Long) row[0];
            var summary = summaries.computeIfAbsent(id, key -> new OrderSummaryDto(
                    key, (String) row[1], 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            var count = ((Number) row[3]).longValue();
            var total = row[4] == null ? BigDecimal.ZERO : (BigDecimal) row[4];

            summary.setOrderCount(summary.getOrderCount() + count);
            summary.setTotal(summary.getTotal().add(total));
            if (row[2] == PaymentMethod.CARD) {
                summary.setCardTotal(summary.getCardTotal().add(total));
            } else if (row[2] == PaymentMethod.CASH) {
                summary.setCashTotal(summary.getCashTotal().add(total));
            }
        }

        var result = new ArrayList<>(summaries.values());
        if (marketPlaceId != null && !marketPlaceId.isBlank()) {
            Long filterId = NO_MARKET_PLACE.equals(marketPlaceId) ? null : Long.valueOf(marketPlaceId);
            result.removeIf(summary -> !Objects.equals(summary.getMarketPlaceId(), filterId));
        }
        result.sort(Comparator.comparing(OrderSummaryDto::getTotal).reversed());
        return result;
    }

    public OrderDto getOrder(Long orderId) {
        var order = orderRepository
                .getOrderWithItems(orderId)
                .orElseThrow(OrderNotFoundException::new);

        var user = authService.getCurrentUser();
        if (!order.isPlacedBy(user)) {
            throw new OrderAccessDeniedException();
        }

        return orderMapper.toDto(order);
    }
}
