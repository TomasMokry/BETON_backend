package org.tomo.beton.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tomo.beton.dtos.CartDto;
import org.tomo.beton.dtos.CartItemDto;
import org.tomo.beton.entities.Cart;
import org.tomo.beton.entities.CartItem;
import org.tomo.beton.entities.Product;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.ProductNotFoundException;
import org.tomo.beton.mappers.CartMapper;
import org.tomo.beton.repositories.CartRepository;
import org.tomo.beton.repositories.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private CartService cartService;

    private final UUID cartId = UUID.randomUUID();

    private static Product product(long id, int amount) {
        var product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(BigDecimal.TEN);
        product.setAmount(amount);
        return product;
    }

    @Test
    void createCart_savesAndReturnsDto() {
        var dto = new CartDto();
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        assertThat(cartService.createCart()).isSameAs(dto);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addToCart_addsProductAndSaves() {
        var cart = new Cart();
        var product = product(1, 5);
        var dto = new CartItemDto();
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toDto(any(CartItem.class))).thenReturn(dto);

        var result = cartService.addToCart(cartId, 1L);

        assertThat(result).isSameAs(dto);
        assertThat(cart.getItem(1L).getQuantity()).isEqualTo(1);
        verify(cartRepository).save(cart);
    }

    @Test
    void addToCart_cartNotFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(cartId, 1L)).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void addToCart_productNotFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(new Cart()));
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(cartId, 1L)).isInstanceOf(ProductNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getCart_found_returnsDto() {
        var cart = new Cart();
        var dto = new CartDto();
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(dto);

        assertThat(cartService.getCart(cartId)).isSameAs(dto);
    }

    @Test
    void getCart_notFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(cartId)).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void updateItem_setsQuantityAndSaves() {
        var cart = new Cart();
        cart.addItem(product(1, 5));
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

        cartService.updateItem(cartId, 1L, 3);

        assertThat(cart.getItem(1L).getQuantity()).isEqualTo(3);
        verify(cartRepository).save(cart);
    }

    @Test
    void updateItem_cartNotFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItem(cartId, 1L, 3)).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void updateItem_productNotInCart_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(new Cart()));

        assertThatThrownBy(() -> cartService.updateItem(cartId, 1L, 3)).isInstanceOf(ProductNotFoundException.class);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void removeItem_removesAndSaves() {
        var cart = new Cart();
        cart.addItem(product(1, 5));
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

        cartService.removeItem(cartId, 1L);

        assertThat(cart.isEmpty()).isTrue();
        verify(cartRepository).save(cart);
    }

    @Test
    void removeItem_cartNotFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(cartId, 1L)).isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void clearCart_clearsAndSaves() {
        var cart = new Cart();
        cart.addItem(product(1, 5));
        cart.addItem(product(2, 5));
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

        cartService.clearCart(cartId);

        assertThat(cart.isEmpty()).isTrue();
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_cartNotFound_throws() {
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.clearCart(cartId)).isInstanceOf(CartNotFoundException.class);
    }
}
