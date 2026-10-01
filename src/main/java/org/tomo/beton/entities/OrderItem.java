package org.tomo.beton.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tomo.beton.dtos.DiscountPercent;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    public OrderItem(Order order, Product product, Integer quantity, Integer discountPercent) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.discountPercent = discountPercent;
        this.unitPrice = product.getPrice();
        this.totalPrice = DiscountPercent.apply(unitPrice.multiply(BigDecimal.valueOf(quantity)), discountPercent);
    }
}