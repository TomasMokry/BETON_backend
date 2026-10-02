package org.tomo.beton.entities;

import org.junit.jupiter.api.Test;
import org.tomo.beton.excetions.InvalidDiscountException;
import org.tomo.beton.excetions.ProductOutOfStockException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private static Product product(long id, String name, String price, int amount) {
        var product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setAmount(amount);
        return product;
    }

    @Test
    void addItem_newProduct_createsItemWithQuantityOne() {
        var cart = new Cart();
        var product = product(1, "Vase", "100", 5);

        var item = cart.addItem(product);

        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getProduct()).isSameAs(product);
        assertThat(item.getCart()).isSameAs(cart);
        assertThat(cart.getItems()).containsExactly(item);
    }

    @Test
    void addItem_sameProductTwice_incrementsQuantity() {
        var cart = new Cart();
        var product = product(1, "Vase", "100", 5);

        cart.addItem(product);
        var item = cart.addItem(product);

        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void addItem_productWithNoStock_throws() {
        var cart = new Cart();

        assertThatThrownBy(() -> cart.addItem(product(1, "Vase", "100", 0)))
                .isInstanceOf(ProductOutOfStockException.class);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void addItem_quantityWouldExceedStock_throws() {
        var cart = new Cart();
        var product = product(1, "Vase", "100", 1);
        cart.addItem(product);

        assertThatThrownBy(() -> cart.addItem(product))
                .isInstanceOf(ProductOutOfStockException.class);
        assertThat(cart.getItem(1L).getQuantity()).isEqualTo(1);
    }

    @Test
    void updateItemQuantity_withinStock_setsQuantity() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));

        cart.updateItemQuantity(1L, 5);

        assertThat(cart.getItem(1L).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItemQuantity_aboveStock_throws() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));

        assertThatThrownBy(() -> cart.updateItemQuantity(1L, 6))
                .isInstanceOf(ProductOutOfStockException.class);
        assertThat(cart.getItem(1L).getQuantity()).isEqualTo(1);
    }

    @Test
    void removeItem_removesOnlyThatProduct() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));
        cart.addItem(product(2, "Tray", "50", 5));

        cart.removeItem(1L);

        assertThat(cart.getItem(1L)).isNull();
        assertThat(cart.getItem(2L)).isNotNull();
    }

    @Test
    void removeItem_unknownProduct_doesNothing() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));

        cart.removeItem(99L);

        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void clear_emptiesCart() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));

        cart.clear();

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void getTotalPrice_sumsPriceTimesQuantity() {
        var cart = new Cart();
        var vase = product(1, "Vase", "100.50", 5);
        cart.addItem(vase);
        cart.addItem(vase);
        cart.addItem(product(2, "Tray", "20", 5));

        assertThat(cart.getTotalPrice()).isEqualByComparingTo("221.00");
    }

    @Test
    void getTotalPrice_emptyCart_isZero() {
        assertThat(new Cart().getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void itemDiscount_reducesItemAndCartTotal() {
        var cart = new Cart();
        var vase = product(1, "Vase", "100", 5);
        cart.addItem(vase);
        cart.addItem(vase);
        cart.addItem(product(2, "Tray", "20", 5));

        cart.updateItemDiscount(1L, 15);

        assertThat(cart.getItem(1L).getSubtotalPrice()).isEqualByComparingTo("200.00");
        assertThat(cart.getItem(1L).getTotalPrice()).isEqualByComparingTo("170.00");
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("190.00");
    }

    @Test
    void cartDiscount_appliesToSubtotalAfterItemDiscounts() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));
        cart.addItem(product(2, "Tray", "50", 5));
        cart.updateItemDiscount(1L, 10);

        cart.updateDiscount(20);

        assertThat(cart.getSubtotalPrice()).isEqualByComparingTo("140.00");
        assertThat(cart.getDiscountAmount()).isEqualByComparingTo("28.00");
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("112.00");
    }

    @Test
    void giftDiscount_makesItemAndCartFree() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));
        cart.addItem(product(2, "Tray", "50", 5));

        cart.updateItemDiscount(2L, 100);
        assertThat(cart.getItem(2L).getTotalPrice()).isEqualByComparingTo("0");
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("100.00");

        cart.updateDiscount(100);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("0");
    }

    @Test
    void discount_roundsToTwoDecimals() {
        var cart = new Cart();
        cart.addItem(product(1, "Candle", "33.33", 5));

        cart.updateItemDiscount(1L, 5);

        assertThat(cart.getItem(1L).getTotalPrice()).isEqualByComparingTo("31.66");
    }

    @Test
    void discount_notAllowedValue_throws() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));

        assertThatThrownBy(() -> cart.updateDiscount(7))
                .isInstanceOf(InvalidDiscountException.class);
        assertThatThrownBy(() -> cart.updateItemDiscount(1L, 101))
                .isInstanceOf(InvalidDiscountException.class);
        assertThat(cart.getDiscountPercent()).isZero();
        assertThat(cart.getItem(1L).getDiscountPercent()).isZero();
    }

    @Test
    void clear_resetsCartDiscount() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "100", 5));
        cart.updateDiscount(10);

        cart.clear();

        assertThat(cart.getDiscountPercent()).isZero();
    }

    @Test
    void getItems_newestAddedFirst() {
        var cart = new Cart();
        // ids are assigned by the database in the order items are added
        cart.addItem(product(1, "vase", "1", 5)).setId(10L);
        cart.addItem(product(2, "Candle", "1", 5)).setId(11L);
        cart.addItem(product(3, "bowl", "1", 5)).setId(12L);

        assertThat(cart.getItems())
                .extracting(item -> item.getProduct().getName())
                .containsExactly("bowl", "Candle", "vase");
    }

    @Test
    void getItems_unsavedItemComesFirst() {
        var cart = new Cart();
        cart.addItem(product(1, "Vase", "1", 5)).setId(10L);
        cart.addItem(product(2, "Bowl", "1", 5));

        assertThat(cart.getItems())
                .extracting(item -> item.getProduct().getName())
                .containsExactly("Bowl", "Vase");
    }
}
