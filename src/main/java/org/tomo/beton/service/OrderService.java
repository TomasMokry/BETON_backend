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

@AllArgsConstructor
@Service
public class OrderService {
    /** Value of the marketPlaceId filter that selects orders sold outside any market place. */
    public static final String NO_MARKET_PLACE = "none";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AuthService authService;

    /** Parsed marketPlaceId request parameter. */
    private record MarketFilter(boolean filterMarket, boolean noMarket, Long marketPlaceId) {
        static MarketFilter parse(String marketPlaceId) {
            if (marketPlaceId == null || marketPlaceId.isBlank()) {
                return new MarketFilter(false, false, null);
            }
            if (NO_MARKET_PLACE.equals(marketPlaceId)) {
                return new MarketFilter(true, true, null);
            }
            return new MarketFilter(true, false, Long.valueOf(marketPlaceId));
        }
    }

    /**
     * Admins may see every customer's orders (or one customer's via userId);
     * other users always see only their own and may not ask for someone else's.
     */
    private Long resolveCustomerId(Long requestedUserId) {
        if (authService.isCurrentUserAdmin()) {
            return requestedUserId;
        }
        var currentUserId = authService.getCurrentUser().getId();
        if (requestedUserId != null && !requestedUserId.equals(currentUserId)) {
            throw new OrderAccessDeniedException();
        }
        return currentUserId;
    }

    public List<OrderDto> getAllOrders(String marketPlaceId, Long userId) {
        var customerId = resolveCustomerId(userId);
        var market = MarketFilter.parse(marketPlaceId);

        var orders = orderRepository.findFiltered(
                customerId, market.filterMarket(), market.noMarket(), market.marketPlaceId());
        return orders.stream().map(orderMapper::toDto).toList();
    }

    public List<OrderSummaryDto> getSummary(String marketPlaceId, Long userId) {
        var customerId = resolveCustomerId(userId);
        var market = MarketFilter.parse(marketPlaceId);

        var summaries = new LinkedHashMap<Long, OrderSummaryDto>();
        var rows = orderRepository.summarizeByMarketPlace(
                customerId, market.filterMarket(), market.noMarket(), market.marketPlaceId());
        for (var row : rows) {
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
        result.sort(Comparator.comparing(OrderSummaryDto::getTotal).reversed());
        return result;
    }

    public OrderDto getOrder(Long orderId) {
        var order = orderRepository
                .getOrderWithItems(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!authService.isCurrentUserAdmin() && !order.isPlacedBy(authService.getCurrentUser())) {
            throw new OrderAccessDeniedException();
        }

        return orderMapper.toDto(order);
    }
}
