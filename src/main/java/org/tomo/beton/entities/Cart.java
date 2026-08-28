package org.tomo.beton.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tomo.beton.excetions.ProductOutOfStockException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "date_created", insertable = false, updatable = false)
    private LocalDate dateCreated;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.MERGE, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<CartItem> items = new LinkedHashSet<>();

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CartItem getItem(Long productId) {
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public CartItem addItem(Product product) {
        var cartItem = getItem(product.getId());

        if (cartItem != null) {
            if (cartItem.getQuantity() >= product.getAmount()) {
                throw new ProductOutOfStockException();
            }
            cartItem.setQuantity(cartItem.getQuantity() + 1);
        } else {
            if (product.getAmount() <= 0) {
                throw new ProductOutOfStockException();
            }
            cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(1);
            cartItem.setCart(this);
            items.add(cartItem);
        }
        return cartItem;
    }

    public void updateItemQuantity(Long productId, Integer quantity) {
        var cartItem = getItem(productId);
        if(quantity > cartItem.getProduct().getAmount()){
            throw new ProductOutOfStockException();
        }
        cartItem.setQuantity(quantity);
    }

    public void removeItem(Long productId) {
        var cartItem = getItem(productId);
        if (cartItem != null) {
            items.remove(cartItem);
            cartItem.setCart(null);
        }
    }

    public Set<CartItem> getItems() {
        return items.stream()
                .sorted(Comparator.comparing(
                        item -> item.getProduct().getName(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void clear() {
        items.clear();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
