package org.tomo.beton.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.CheckoutResponse;
import org.tomo.beton.entities.Order;
import org.tomo.beton.excetions.CartEmptyException;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.ProductNotFoundException;
import org.tomo.beton.excetions.ProductOutOfStockException;
import org.tomo.beton.repositories.CartRepository;
import org.tomo.beton.repositories.OrderRepository;
import org.tomo.beton.repositories.ProductRepository;

@Service
@AllArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AuthService authService;
    private final CartService cartService;

    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        var cart = cartRepository.getCartWithItems(request.getCartId())
                .orElseThrow(CartNotFoundException::new);

        if (cart.isEmpty()) {
            throw new CartEmptyException();
        }

        for (var cartItem : cart.getItems()) {
            var product = productRepository.findById(cartItem.getProduct().getId())
                    .orElseThrow(ProductNotFoundException::new);

            if (product.getAmount() < cartItem.getQuantity()) {
                throw new ProductOutOfStockException();
            }

            product.setAmount(
                    product.getAmount() - cartItem.getQuantity()
            );

            productRepository.save(product);
        }

        var order = Order.fromCart(
                cart,
                request.getPaymentMethod(),
                authService.getCurrentUser()
        );

        orderRepository.save(order);
        cartService.clearCart(cart.getId());

        return new CheckoutResponse(order.getId());
    }
}
