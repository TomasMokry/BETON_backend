package org.tomo.beton.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.AddItemToCartRequest;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.PaymentMethod;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.dtos.UpdateDiscountRequest;
import org.tomo.beton.entities.Product;
import org.tomo.beton.support.AbstractE2ETest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckoutE2ETest extends AbstractE2ETest {

    private String token;
    private Product vase;

    @BeforeEach
    void setUp() throws Exception {
        token = tokenFor("tom@mail.com", Role.USER);
        vase = createProduct("Vase", createCategory("Vases"), "100.00", 5);
    }

    private String createCart() throws Exception {
        var response = mockMvc.perform(post("/carts")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void addItem(String cartId, Long productId) throws Exception {
        var request = new AddItemToCartRequest();
        request.setProductId(productId);
        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated());
    }

    private String checkoutBody(String cartId) throws Exception {
        var request = new CheckoutRequest();
        request.setCartId(cartId == null ? null : UUID.fromString(cartId));
        request.setPaymentMethod("CARD");
        return json(request);
    }

    private void setDiscount(String path, int percent) throws Exception {
        var request = new UpdateDiscountRequest();
        request.setDiscountPercent(percent);
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk());
    }

    @Test
    void checkout_storesDiscountsOnOrderAndResetsCartDiscount() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, vase.getId());
        setDiscount("/carts/" + cartId + "/items/" + vase.getId() + "/discount", 10);
        setDiscount("/carts/" + cartId + "/discount", 5);

        var response = mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer orderId = JsonPath.read(response, "$.orderId");
        var order = orderRepository.getOrderWithItems(orderId.longValue()).orElseThrow();
        assertThat(order.getSubtotalPrice()).isEqualByComparingTo("180.00");
        assertThat(order.getDiscountPercent()).isEqualTo(5);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("171.00");
        var item = order.getItems().iterator().next();
        assertThat(item.getDiscountPercent()).isEqualTo(10);
        assertThat(item.getTotalPrice()).isEqualByComparingTo("180.00");

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.discountPercent").value(0));
    }

    @Test
    void checkout_createsOrderDecreasesStockAndEmptiesCart() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, vase.getId());

        var response = mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNumber())
                .andReturn().getResponse().getContentAsString();

        Integer orderId = JsonPath.read(response, "$.orderId");
        var order = orderRepository.getOrderWithItems(orderId.longValue()).orElseThrow();
        var customerId = userRepository.findByEmail("tom@mail.com").orElseThrow().getId();
        assertThat(order.getCustomer().getId()).isEqualTo(customerId);
        assertThat(order.getMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("200.00");
        // application-test.yaml sets beton.card-fee.percent: 1.5
        assertThat(order.getCardFee()).isEqualByComparingTo("3.00");
        assertThat(order.getNetPrice()).isEqualByComparingTo("197.00");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().iterator().next().getQuantity()).isEqualTo(2);

        assertThat(productRepository.findById(vase.getId()).orElseThrow().getAmount()).isEqualTo(3);

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void checkout_emptyCart_returns400() throws Exception {
        var cartId = createCart();

        mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cart is empty"));

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void checkout_unknownCart_returns400() throws Exception {
        mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cart not found"));
    }

    @Test
    void checkout_stockDroppedAfterAddingToCart_returns400AndChangesNothing() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, vase.getId());
        vase.setAmount(1);
        productRepository.save(vase);

        mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product is out of stock"));

        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.findById(vase.getId()).orElseThrow().getAmount()).isEqualTo(1);
        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void checkout_missingCartId_returns400() throws Exception {
        mockMvc.perform(post("/checkout")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cartId").value("CartId is required"));
    }

    @Test
    void checkout_withoutToken_returns401() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());

        mockMvc.perform(post("/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutBody(cartId)))
                .andExpect(status().isUnauthorized());

        assertThat(orderRepository.count()).isZero();
    }
}
