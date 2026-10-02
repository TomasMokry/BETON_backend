package org.tomo.beton.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tomo.beton.dtos.CartDto;
import org.tomo.beton.dtos.OrderDto;
import org.tomo.beton.dtos.OrderSummaryDto;
import org.tomo.beton.dtos.PaymentMethod;
import org.tomo.beton.entities.Order;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.OrderAccessDeniedException;
import org.tomo.beton.excetions.OrderNotFoundException;
import org.tomo.beton.mappers.CartMapper;
import org.tomo.beton.mappers.OrderMapper;
import org.tomo.beton.repositories.CartRepository;
import org.tomo.beton.repositories.OrderRepository;
import org.tomo.beton.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class OrderService {
    /** Value of the marketPlaceId filter that selects orders sold outside any market place. */
    public static final String NO_MARKET_PLACE = "none";

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AuthService authService;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;

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
        return orderMapper.toDto(loadAccessible(orderId));
    }

    /** Deletes the order and returns its items to stock. */
    @Transactional
    public void deleteOrder(Long orderId) {
        var order = loadAccessible(orderId);
        returnStock(order);
        orderRepository.delete(order);
    }

    /**
     * Puts the order's items (quantities and discounts) back into the given cart, replacing its content,
     * returns the stock and deletes the order, so it can be corrected and checked out again.
     */
    @Transactional
    public CartDto reopenOrder(Long orderId, UUID cartId) {
        var order = loadAccessible(orderId);
        var cart = cartRepository.getCartWithItems(cartId).orElseThrow(CartNotFoundException::new);

        returnStock(order);

        // Lines for the order's products are updated in place: re-inserting them would hit the
        // unique (cart, product) constraint, because Hibernate flushes inserts before orphan deletes
        var orderProductIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());
        cart.getItems().stream()
                .filter(item -> !orderProductIds.contains(item.getProduct().getId()))
                .forEach(item -> cart.removeItem(item.getProduct().getId()));
        order.getItems().forEach(item ->
                cart.restoreItem(item.getProduct(), item.getQuantity(), item.getDiscountPercent()));
        cart.updateDiscount(order.getDiscountPercent());
        cartRepository.save(cart);

        orderRepository.delete(order);
        return cartMapper.toDto(cart);
    }

    /** Loads an order the current user may see and change: admins any, others only their own. */
    private Order loadAccessible(Long orderId) {
        var order = orderRepository
                .getOrderWithItems(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!authService.isCurrentUserAdmin() && !order.isPlacedBy(authService.getCurrentUser())) {
            throw new OrderAccessDeniedException();
        }
        return order;
    }

    private void returnStock(Order order) {
        for (var item : order.getItems()) {
            var product = item.getProduct();
            product.setAmount(product.getAmount() + item.getQuantity());
            productRepository.save(product);
        }
    }
}
