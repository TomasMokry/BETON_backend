package org.tomo.beton.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.PaymentMethod;
import org.tomo.beton.entities.Cart;
import org.tomo.beton.entities.Order;
import org.tomo.beton.entities.Product;
import org.tomo.beton.entities.User;
import org.tomo.beton.excetions.CartEmptyException;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.ProductNotFoundException;
import org.tomo.beton.excetions.ProductOutOfStockException;
import org.tomo.beton.repositories.CartRepository;
import org.tomo.beton.repositories.OrderRepository;
import org.tomo.beton.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AuthService authService;
    @Mock
    private CartService cartService;
    @InjectMocks
    private CheckoutService checkoutService;

    private final UUID cartId = UUID.randomUUID();

    private static Product product(long id, int amount) {
        var product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(new BigDecimal("10"));
        product.setAmount(amount);
        return product;
    }

    private CheckoutRequest request() {
        var request = new CheckoutRequest();
        request.setCartId(cartId);
        request.setPaymentMethod("CARD");
        return request;
    }

    private Cart cartWith(Product product, int quantity) {
        var cart = new Cart();
        cart.setId(cartId);
        cart.addItem(product);
        cart.updateItemQuantity(product.getId(), quantity);
        return cart;
    }

    @Test
    void checkout_cartNotFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(request())).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void checkout_emptyCart_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(new Cart()));

        assertThatThrownBy(() -> checkoutService.checkout(request())).isInstanceOf(CartEmptyException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_productDeleted_throws() {
        var cart = cartWith(product(1, 5), 2);
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.checkout(request())).isInstanceOf(ProductNotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_stockDroppedBelowCartQuantity_throws() {
        var cart = cartWith(product(1, 5), 3);
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        // Stock changed since the item was put in the cart
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1, 2)));

        assertThatThrownBy(() -> checkoutService.checkout(request())).isInstanceOf(ProductOutOfStockException.class);
        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clearCart(any());
    }

    @Test
    void checkout_success_decreasesStockSavesOrderAndClearsCart() {
        var product = product(1, 5);
        var cart = cartWith(product, 3);
        var customer = User.builder().id(1L).build();
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(authService.getCurrentUser()).thenReturn(customer);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(42L);
            return order;
        });

        var response = checkoutService.checkout(request());

        assertThat(response.getOrderId()).isEqualTo(42L);
        assertThat(product.getAmount()).isEqualTo(2);
        verify(productRepository).save(product);

        var order = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(order.capture());
        assertThat(order.getValue().getCustomer()).isSameAs(customer);
        assertThat(order.getValue().getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(order.getValue().getTotalPrice()).isEqualByComparingTo("30");

        verify(cartService).clearCart(cartId);
    }
}
