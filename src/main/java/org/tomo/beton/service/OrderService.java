package org.tomo.beton.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tomo.beton.dtos.OrderDto;
import org.tomo.beton.excetions.OrderAccessDeniedException;
import org.tomo.beton.excetions.OrderNotFoundException;
import org.tomo.beton.mappers.OrderMapper;
import org.tomo.beton.repositories.OrderRepository;

import java.util.List;

@AllArgsConstructor
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final AuthService authService;

    public List<OrderDto> getAllOrders() {
        var user = authService.getCurrentUser();

        var orders = orderRepository.getOrdersByCustomer(user);
        return orders.stream().map(orderMapper::toDto).toList();
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
