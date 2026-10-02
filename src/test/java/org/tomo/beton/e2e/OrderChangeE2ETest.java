package org.tomo.beton.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.AddItemToCartRequest;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.ReopenOrderRequest;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.dtos.UpdateDiscountRequest;
import org.tomo.beton.entities.Cart;
import org.tomo.beton.entities.Order;
import org.tomo.beton.entities.Product;
import org.tomo.beton.entities.User;
import org.tomo.beton.support.AbstractE2ETest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderChangeE2ETest extends AbstractE2ETest {

    private String token;
    private Product vase;
    private Product bowl;

    @BeforeEach
    void setUp() throws Exception {
        token = tokenFor("tom@mail.com", Role.USER);
        var category = createCategory("Mixed");
        vase = createProduct("Vase", category, "100.00", 10);
        bowl = createProduct("Bowl", category, "25.50", 10);
    }

    private String createCart() throws Exception {
        var response = mockMvc.perform(post("/carts")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void addItem(String cartId, Product product) throws Exception {
        var item = new AddItemToCartRequest();
        item.setProductId(product.getId());
        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(item)))
                .andExpect(status().isCreated());
    }

    private void setDiscount(String path, int percent) throws Exception {
        var request = new UpdateDiscountRequest();
        request.setDiscountPercent(percent);
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk());
    }

    private long checkout(String cartId) throws Exception {
        var checkout = new CheckoutRequest();
        checkout.setCartId(UUID.fromString(cartId));
        checkout.setPaymentMethod("CARD");
        var response = mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checkout)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Integer) JsonPath.read(response, "$.orderId")).longValue();
    }

    /** 2x Vase (10 % off) + 1x Bowl, 5 % cart discount. Leaves stock at Vase 8, Bowl 9. */
    private long placeOrder() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase);
        addItem(cartId, vase);
        addItem(cartId, bowl);
        setDiscount("/carts/" + cartId + "/items/" + vase.getId() + "/discount", 10);
        setDiscount("/carts/" + cartId + "/discount", 5);
        return checkout(cartId);
    }

    private String reopenBody(String cartId) throws Exception {
        var request = new ReopenOrderRequest();
        request.setCartId(cartId == null ? null : UUID.fromString(cartId));
        return json(request);
    }

    private int stockOf(Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getAmount();
    }

    private long orderOf(User customer) {
        var cart = new Cart();
        cart.addItem(bowl);
        return orderRepository.save(Order.fromCart(cart, "CASH", customer)).getId();
    }

    @Test
    void delete_removesOrderAndReturnsStock() throws Exception {
        var orderId = placeOrder();
        assertThat(stockOf(vase)).isEqualTo(8);
        assertThat(stockOf(bowl)).isEqualTo(9);

        mockMvc.perform(delete("/orders/" + orderId).with(bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(stockOf(vase)).isEqualTo(10);
        assertThat(stockOf(bowl)).isEqualTo(10);
        mockMvc.perform(get("/orders/" + orderId).with(bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/orders/summary").with(bearer(token)))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void delete_otherUsersOrder_userGets403_adminGets204() throws Exception {
        var other = createUser("other@mail.com", "password123", Role.USER);
        var orderId = orderOf(other);

        mockMvc.perform(delete("/orders/" + orderId).with(bearer(token)))
                .andExpect(status().isForbidden());
        assertThat(orderRepository.existsById(orderId)).isTrue();

        var adminToken = tokenFor("admin@mail.com", Role.ADMIN);
        mockMvc.perform(delete("/orders/" + orderId).with(bearer(adminToken)))
                .andExpect(status().isNoContent());
        assertThat(orderRepository.existsById(orderId)).isFalse();
    }

    @Test
    void delete_missingOrder_returns404() throws Exception {
        mockMvc.perform(delete("/orders/999").with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reopen_replacesCartWithOrderItemsAndDiscounts_deletesOrder_returnsStock() throws Exception {
        var orderId = placeOrder();

        // The target cart already holds a Bowl (also in the order) and gets replaced
        var cartId = createCart();
        addItem(cartId, bowl);
        addItem(cartId, bowl);

        mockMvc.perform(post("/orders/" + orderId + "/reopen")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenBody(cartId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.discountPercent").value(5));

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Vase')].quantity").value(2))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Vase')].discountPercent").value(10))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Bowl')].quantity").value(1))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Bowl')].discountPercent").value(0))
                .andExpect(jsonPath("$.discountPercent").value(5));

        assertThat(orderRepository.existsById(orderId)).isFalse();
        assertThat(stockOf(vase)).isEqualTo(10);
        assertThat(stockOf(bowl)).isEqualTo(10);
    }

    @Test
    void reopen_removesCartItemsNotInOrder() throws Exception {
        var cart = new Cart();
        cart.addItem(vase);
        var orderId = orderRepository.save(Order.fromCart(cart, "CASH",
                userRepository.findByEmail("tom@mail.com").orElseThrow())).getId();

        var cartId = createCart();
        addItem(cartId, bowl);

        mockMvc.perform(post("/orders/" + orderId + "/reopen")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenBody(cartId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].product.name").value("Vase"));
    }

    @Test
    void reopen_thenCheckout_createsNewOrderAndTakesStockAgain() throws Exception {
        var orderId = placeOrder();
        var cartId = createCart();

        mockMvc.perform(post("/orders/" + orderId + "/reopen")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenBody(cartId)))
                .andExpect(status().isOk());

        var newOrderId = checkout(cartId);

        assertThat(newOrderId).isNotEqualTo(orderId);
        assertThat(stockOf(vase)).isEqualTo(8);
        assertThat(stockOf(bowl)).isEqualTo(9);
        mockMvc.perform(get("/orders").with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(newOrderId))
                .andExpect(jsonPath("$[0].discountPercent").value(5));
    }

    @Test
    void reopen_unknownCart_returns400_andChangesNothing() throws Exception {
        var orderId = placeOrder();

        mockMvc.perform(post("/orders/" + orderId + "/reopen")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenBody(UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.existsById(orderId)).isTrue();
        assertThat(stockOf(vase)).isEqualTo(8);
    }

    @Test
    void reopen_otherUsersOrder_returns403() throws Exception {
        var other = createUser("other@mail.com", "password123", Role.USER);
        var orderId = orderOf(other);

        mockMvc.perform(post("/orders/" + orderId + "/reopen")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reopenBody(createCart())))
                .andExpect(status().isForbidden());
        assertThat(orderRepository.existsById(orderId)).isTrue();
    }
}
