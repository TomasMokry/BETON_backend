package org.tomo.beton.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.tomo.beton.dtos.PaymentMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    @ManyToOne
    @JoinColumn(name = "marketplace_id")
    private MarketPlace marketPlace;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "subtotal_price")
    private BigDecimal subtotalPrice;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "card_fee")
    private BigDecimal cardFee = BigDecimal.ZERO;

    @Column(name = "net_price")
    private BigDecimal netPrice;

    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Set<OrderItem> items = new LinkedHashSet<>();

    public static Order fromCart(Cart cart, String paymentMethod, User customer) {
        var order = new Order();
        order.setCustomer(customer);
        order.setMarketPlace(customer.getCurrentMarketPlace());
        order.setMethod(PaymentMethod.valueOf(paymentMethod));
        order.setSubtotalPrice(cart.getSubtotalPrice());
        order.setDiscountPercent(cart.getDiscountPercent());
        order.setTotalPrice(cart.getTotalPrice());
        order.setNetPrice(order.getTotalPrice());

        cart.getItems().forEach(item -> {
            var orderItem = new OrderItem(order, item.getProduct(), item.getQuantity(), item.getDiscountPercent());
            order.items.add(orderItem);
        });

        return order;
    }

    /** Deducts the bank fee (percent of the total) for card payments; other methods keep the full total. */
    public void applyCardFee(BigDecimal percent) {
        if (method != PaymentMethod.CARD || percent == null || percent.signum() <= 0) {
            cardFee = BigDecimal.ZERO.setScale(2);
        } else {
            cardFee = totalPrice.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        netPrice = totalPrice.subtract(cardFee);
    }

    public boolean isPlacedBy(User customer) {
        return customer != null && this.customer.getId().equals(customer.getId());
    }
}
