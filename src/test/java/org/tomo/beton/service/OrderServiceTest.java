package org.tomo.beton.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tomo.beton.dtos.OrderDto;
import org.tomo.beton.entities.Order;
import org.tomo.beton.entities.User;
import org.tomo.beton.excetions.OrderAccessDeniedException;
import org.tomo.beton.excetions.OrderNotFoundException;
import org.tomo.beton.mappers.OrderMapper;
import org.tomo.beton.repositories.OrderRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private AuthService authService;
    @InjectMocks
    private OrderService orderService;

    private final User currentUser = User.builder().id(1L).build();
    private final User otherUser = User.builder().id(2L).build();

    private static Order orderOf(User customer) {
        var order = new Order();
        order.setId(10L);
        order.setCustomer(customer);
        return order;
    }

    @Test
    void getAllOrders_returnsOrdersOfCurrentUser() {
        var order = orderOf(currentUser);
        var dto = new OrderDto();
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(orderRepository.findFiltered(1L, false, false, null)).thenReturn(List.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);

        assertThat(orderService.getAllOrders(null, null)).containsExactly(dto);
    }

    @Test
    void getAllOrders_userAsksForOtherUser_throws() {
        when(authService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> orderService.getAllOrders(null, 2L))
                .isInstanceOf(OrderAccessDeniedException.class);
    }

    @Test
    void getAllOrders_admin_canFilterByUserAndMarket() {
        var order = orderOf(otherUser);
        var dto = new OrderDto();
        when(authService.isCurrentUserAdmin()).thenReturn(true);
        when(orderRepository.findFiltered(2L, true, false, 5L)).thenReturn(List.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);

        assertThat(orderService.getAllOrders("5", 2L)).containsExactly(dto);
    }

    @Test
    void getAllOrders_admin_withoutUser_returnsEveryonesOrders() {
        var order = orderOf(otherUser);
        var dto = new OrderDto();
        when(authService.isCurrentUserAdmin()).thenReturn(true);
        when(orderRepository.findFiltered(null, true, true, null)).thenReturn(List.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);

        assertThat(orderService.getAllOrders("none", null)).containsExactly(dto);
    }

    @Test
    void getOrder_admin_canOpenOtherUsersOrder() {
        var order = orderOf(otherUser);
        var dto = new OrderDto();
        when(orderRepository.getOrderWithItems(10L)).thenReturn(Optional.of(order));
        when(authService.isCurrentUserAdmin()).thenReturn(true);
        when(orderMapper.toDto(order)).thenReturn(dto);

        assertThat(orderService.getOrder(10L)).isSameAs(dto);
    }

    @Test
    void getOrder_ownOrder_returnsDto() {
        var order = orderOf(currentUser);
        var dto = new OrderDto();
        when(orderRepository.getOrderWithItems(10L)).thenReturn(Optional.of(order));
        when(authService.getCurrentUser()).thenReturn(currentUser);
        when(orderMapper.toDto(order)).thenReturn(dto);

        assertThat(orderService.getOrder(10L)).isSameAs(dto);
    }

    @Test
    void getOrder_notFound_throws() {
        when(orderRepository.getOrderWithItems(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(10L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrder_otherUsersOrder_throws() {
        when(orderRepository.getOrderWithItems(10L)).thenReturn(Optional.of(orderOf(otherUser)));
        when(authService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> orderService.getOrder(10L)).isInstanceOf(OrderAccessDeniedException.class);
    }
}
