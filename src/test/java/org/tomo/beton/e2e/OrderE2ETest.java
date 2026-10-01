package org.tomo.beton.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.AddItemToCartRequest;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.entities.Cart;
import org.tomo.beton.entities.Order;
import org.tomo.beton.entities.Product;
import org.tomo.beton.entities.User;
import org.tomo.beton.support.AbstractE2ETest;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderE2ETest extends AbstractE2ETest {

    private String token;
    private User otherUser;
    private Product vase;
    private Product bowl;

    @BeforeEach
    void setUp() throws Exception {
        // AuthService currently resolves the customer as user id 1, so the logged-in user is created first
        token = tokenFor("tom@mail.com", Role.USER);
        otherUser = createUser("other@mail.com", "password123", Role.USER);
        var category = createCategory("Mixed");
        vase = createProduct("Vase", category, "100.00", 10);
        bowl = createProduct("Bowl", category, "25.50", 10);
    }

    /** Places an order for the logged-in user through the real cart + checkout endpoints. */
    private long placeOrder(Product... products) throws Exception {
        var cartResponse = mockMvc.perform(post("/carts")).andReturn().getResponse().getContentAsString();
        String cartId = JsonPath.read(cartResponse, "$.id");
        for (var product : products) {
            var item = new AddItemToCartRequest();
            item.setProductId(product.getId());
            mockMvc.perform(post("/carts/" + cartId + "/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(item)));
        }

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

    /** Stores an order for another customer directly in the database. */
    private long orderOfOtherUser() {
        var cart = new Cart();
        cart.addItem(bowl);
        return orderRepository.save(Order.fromCart(cart, "CASH", otherUser)).getId();
    }

    @Test
    void getAllOrders_returnsOnlyCurrentUsersOrders() throws Exception {
        var first = placeOrder(vase);
        var second = placeOrder(vase, bowl);
        orderOfOtherUser();

        mockMvc.perform(get("/orders").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == %d)].totalPrice", first).value(100.00))
                .andExpect(jsonPath("$[?(@.id == %d)].totalPrice", second).value(125.50))
                .andExpect(jsonPath("$[?(@.id == %d)].items.length()", second).value(2));
    }

    @Test
    void getAllOrders_noOrders_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/orders").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getOrder_ownOrder_returnsOrderWithItems() throws Exception {
        var orderId = placeOrder(vase, vase, bowl);

        mockMvc.perform(get("/orders/" + orderId).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.totalPrice").value(225.50))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Vase')].quantity").value(2))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Vase')].totalPrice").value(200.00))
                .andExpect(jsonPath("$.items[?(@.product.name == 'Bowl')].quantity").value(1));
    }

    @Test
    void getOrder_otherUsersOrder_returns403() throws Exception {
        var orderId = orderOfOtherUser();

        mockMvc.perform(get("/orders/" + orderId).with(bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrder_missing_returns404() throws Exception {
        mockMvc.perform(get("/orders/999").with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void orders_withoutToken_return401() throws Exception {
        var orderId = placeOrder(vase);

        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isUnauthorized());
    }
}
