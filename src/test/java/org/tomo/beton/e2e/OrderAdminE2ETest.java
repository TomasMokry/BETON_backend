package org.tomo.beton.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tomo.beton.dtos.MarketPlaceType;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.entities.Cart;
import org.tomo.beton.entities.MarketPlace;
import org.tomo.beton.entities.Order;
import org.tomo.beton.entities.Product;
import org.tomo.beton.entities.User;
import org.tomo.beton.repositories.MarketPlaceRepository;
import org.tomo.beton.support.AbstractE2ETest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderAdminE2ETest extends AbstractE2ETest {

    @Autowired
    private MarketPlaceRepository marketPlaceRepository;

    private String adminToken;
    private String sellerToken;
    private User seller;
    private User otherSeller;
    private MarketPlace brno;
    private Product vase;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = tokenFor("admin@mail.com", Role.ADMIN);
        sellerToken = tokenFor("seller@mail.com", Role.USER);
        seller = userRepository.findByEmail("seller@mail.com").orElseThrow();
        otherSeller = createUser("other@mail.com", "password123", Role.USER);
        vase = createProduct("Vase", createCategory("Vases"), "100.00", 10);

        var market = new MarketPlace();
        market.setName("Christmas market Brno");
        market.setType(MarketPlaceType.CHRISTMAS);
        brno = marketPlaceRepository.save(market);
    }

    /** Stores an order for the given customer directly, optionally at a market place. */
    private long orderOf(User customer, MarketPlace marketPlace, String method) {
        var cart = new Cart();
        cart.addItem(vase);
        var order = Order.fromCart(cart, method, customer);
        order.setMarketPlace(marketPlace);
        return orderRepository.save(order).getId();
    }

    @Test
    void admin_seesEveryonesOrders_withCustomerName() throws Exception {
        orderOf(seller, brno, "CARD");
        orderOf(otherSeller, null, "CASH");

        mockMvc.perform(get("/orders").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.customer.id == %d)].customer.name", seller.getId())
                        .value("User seller@mail.com"));
    }

    @Test
    void admin_filtersByUserAndMarketPlace() throws Exception {
        var sellerAtBrno = orderOf(seller, brno, "CARD");
        orderOf(seller, null, "CASH");
        orderOf(otherSeller, brno, "CARD");

        mockMvc.perform(get("/orders?userId=" + seller.getId()).with(bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/orders?marketPlaceId=" + brno.getId()).with(bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/orders?userId=" + seller.getId() + "&marketPlaceId=" + brno.getId())
                        .with(bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sellerAtBrno));
    }

    @Test
    void admin_summaryFollowsUserFilter() throws Exception {
        orderOf(seller, brno, "CARD");
        orderOf(seller, brno, "CASH");
        orderOf(otherSeller, brno, "CARD");

        mockMvc.perform(get("/orders/summary").with(bearer(adminToken)))
                .andExpect(jsonPath("$[0].orderCount").value(3))
                .andExpect(jsonPath("$[0].total").value(300.00));
        mockMvc.perform(get("/orders/summary?userId=" + seller.getId()).with(bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderCount").value(2))
                .andExpect(jsonPath("$[0].cardTotal").value(100.00))
                .andExpect(jsonPath("$[0].cashTotal").value(100.00));
    }

    @Test
    void admin_canOpenOtherUsersOrder() throws Exception {
        var orderId = orderOf(seller, null, "CARD");

        mockMvc.perform(get("/orders/" + orderId).with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.id").value(seller.getId()));
    }

    @Test
    void user_seesOnlyOwnOrders_andCannotAskForOthers() throws Exception {
        orderOf(seller, null, "CARD");
        orderOf(otherSeller, null, "CARD");

        mockMvc.perform(get("/orders").with(bearer(sellerToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customer.id").value(seller.getId()));
        mockMvc.perform(get("/orders?userId=" + seller.getId()).with(bearer(sellerToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/orders?userId=" + otherSeller.getId()).with(bearer(sellerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/orders/summary?userId=" + otherSeller.getId()).with(bearer(sellerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void users_list_isAdminOnly_butCurrentMarketPlaceStaysOpen() throws Exception {
        mockMvc.perform(get("/users").with(bearer(sellerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/users").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mockMvc.perform(get("/users/me/marketplace").with(bearer(sellerToken)))
                .andExpect(status().isNoContent());
    }
}
