package org.tomo.beton.entities;

import org.junit.jupiter.api.Test;
import org.tomo.beton.dtos.PaymentMethod;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static Product product(long id, String name, String price) {
        var product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setAmount(10);
        return product;
    }

    private static User user(long id) {
        return User.builder().id(id).name("User " + id).email("user" + id + "@mail.com").build();
    }

    @Test
    void fromCart_copiesItemsPricesMethodAndCustomer() {
        var cart = new Cart();
        var vase = product(1, "Vase", "100");
        cart.addItem(vase);
        cart.addItem(vase);
        cart.addItem(product(2, "Tray", "25.50"));
        var customer = user(1);

        var order = Order.fromCart(cart, "CARD", customer);

        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("225.50");
        assertThat(order.getItems()).hasSize(2);

        var vaseItem = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(1L)).findFirst().orElseThrow();
        assertThat(vaseItem.getOrder()).isSameAs(order);
        assertThat(vaseItem.getQuantity()).isEqualTo(2);
        assertThat(vaseItem.getUnitPrice()).isEqualByComparingTo("100");
        assertThat(vaseItem.getTotalPrice()).isEqualByComparingTo("200");
    }

    @Test
    void fromCart_copiesDiscounts() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100"));
        cart.addItem(product(2, "Tray", "50"));
        cart.updateItemDiscount(1L, 10);
        cart.updateItemDiscount(2L, 100);
        cart.updateDiscount(5);

        var order = Order.fromCart(cart, "CASH", user(1));

        assertThat(order.getSubtotalPrice()).isEqualByComparingTo("90.00");
        assertThat(order.getDiscountPercent()).isEqualTo(5);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("85.50");

        var vaseItem = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(1L)).findFirst().orElseThrow();
        assertThat(vaseItem.getUnitPrice()).isEqualByComparingTo("100");
        assertThat(vaseItem.getDiscountPercent()).isEqualTo(10);
        assertThat(vaseItem.getTotalPrice()).isEqualByComparingTo("90.00");

        var trayItem = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(2L)).findFirst().orElseThrow();
        assertThat(trayItem.getDiscountPercent()).isEqualTo(100);
        assertThat(trayItem.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void fromCart_unknownPaymentMethod_throws() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100"));

        assertThatThrownBy(() -> Order.fromCart(cart, "BITCOIN", user(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isPlacedBy_comparesCustomerById() {
        var order = new Order();
        order.setCustomer(user(1));

        assertThat(order.isPlacedBy(user(1))).isTrue();
        assertThat(order.isPlacedBy(user(2))).isFalse();
    }
}
