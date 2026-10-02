package org.tomo.beton.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.AddItemToCartRequest;
import org.tomo.beton.dtos.CheckoutRequest;
import org.tomo.beton.dtos.MarketPlaceRequest;
import org.tomo.beton.dtos.MarketPlaceType;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.dtos.SetCurrentMarketPlaceRequest;
import org.tomo.beton.entities.Product;
import org.tomo.beton.support.AbstractE2ETest;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarketPlaceE2ETest extends AbstractE2ETest {

    private static final String BRNO = "Christmas market Brno";

    private String token;
    private Product vase;

    @BeforeEach
    void setUp() throws Exception {
        token = tokenFor("tom@mail.com", Role.USER);
        vase = createProduct("Vase", createCategory("Vases"), "100.00", 10);
    }

    private long createMarketPlace(String name) throws Exception {
        var request = new MarketPlaceRequest();
        request.setName(name);
        request.setAddress("Namesti Svobody, Brno");
        request.setType(MarketPlaceType.CHRISTMAS);
        request.setStartDate(LocalDate.of(2026, 11, 27));
        request.setEndDate(LocalDate.of(2026, 12, 23));
        var response = mockMvc.perform(post("/marketplaces")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        return ((Integer) JsonPath.read(response, "$.id")).longValue();
    }

    private void selectMarketPlace(String userToken, Long marketPlaceId) throws Exception {
        var request = new SetCurrentMarketPlaceRequest();
        request.setMarketPlaceId(marketPlaceId);
        mockMvc.perform(put("/users/me/marketplace")
                .with(bearer(userToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)));
    }

    private long placeOrder(String userToken, String method) throws Exception {
        var cartResponse = mockMvc.perform(post("/carts")).andReturn().getResponse().getContentAsString();
        String cartId = JsonPath.read(cartResponse, "$.id");
        var item = new AddItemToCartRequest();
        item.setProductId(vase.getId());
        mockMvc.perform(post("/carts/" + cartId + "/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(item)));

        var checkout = new CheckoutRequest();
        checkout.setCartId(UUID.fromString(cartId));
        checkout.setPaymentMethod(method);
        var response = mockMvc.perform(post("/checkout")
                        .with(bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(checkout)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Integer) JsonPath.read(response, "$.orderId")).longValue();
    }

    @Test
    void create_endDateBeforeStartDate_returns400() throws Exception {
        var request = new MarketPlaceRequest();
        request.setName("Bad");
        request.setType(MarketPlaceType.FAIR);
        request.setStartDate(LocalDate.of(2026, 12, 10));
        request.setEndDate(LocalDate.of(2026, 12, 1));
        mockMvc.perform(post("/marketplaces")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void currentMarketPlace_isStoredPerUser() throws Exception {
        var brno = createMarketPlace(BRNO);
        var otherToken = tokenFor("other@mail.com", Role.USER);

        mockMvc.perform(get("/users/me/marketplace").with(bearer(token)))
                .andExpect(status().isNoContent());

        selectMarketPlace(token, brno);

        mockMvc.perform(get("/users/me/marketplace").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(BRNO));
        mockMvc.perform(get("/users/me/marketplace").with(bearer(otherToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkout_stampsCurrentMarketPlace_orNullWithoutOne() throws Exception {
        var brno = createMarketPlace(BRNO);
        selectMarketPlace(token, brno);
        var atMarket = placeOrder(token, "CARD");

        selectMarketPlace(token, null);
        var noMarket = placeOrder(token, "CASH");

        assertThat(orderRepository.getOrderWithItems(atMarket).orElseThrow().getMarketPlace().getId()).isEqualTo(brno);
        assertThat(orderRepository.getOrderWithItems(noMarket).orElseThrow().getMarketPlace()).isNull();

        mockMvc.perform(get("/orders/" + atMarket).with(bearer(token)))
                .andExpect(jsonPath("$.marketPlace.name").value(BRNO));
        mockMvc.perform(get("/orders?marketPlaceId=" + brno).with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(atMarket));
        mockMvc.perform(get("/orders?marketPlaceId=none").with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(noMarket));
    }

    @Test
    void summary_totalsPerMarketPlaceAndMethod() throws Exception {
        var brno = createMarketPlace(BRNO);
        selectMarketPlace(token, brno);
        placeOrder(token, "CARD");
        placeOrder(token, "CASH");
        placeOrder(token, "CARD");
        selectMarketPlace(token, null);
        placeOrder(token, "CASH");

        mockMvc.perform(get("/orders/summary").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].marketPlaceId").value(brno))
                .andExpect(jsonPath("$[0].orderCount").value(3))
                .andExpect(jsonPath("$[0].cardTotal").value(200.00))
                .andExpect(jsonPath("$[0].cashTotal").value(100.00))
                .andExpect(jsonPath("$[0].total").value(300.00))
                .andExpect(jsonPath("$[1].marketPlaceId").isEmpty())
                .andExpect(jsonPath("$[1].orderCount").value(1));
    }

    @Test
    void archive_clearsCurrentSelectionAndHidesFromActiveList() throws Exception {
        var brno = createMarketPlace(BRNO);
        selectMarketPlace(token, brno);

        mockMvc.perform(patch("/marketplaces/" + brno + "/archive").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/users/me/marketplace").with(bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/marketplaces").with(bearer(token)))
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/marketplaces?includeArchived=true").with(bearer(token)))
                .andExpect(jsonPath("$.length()").value(1));

        var request = new SetCurrentMarketPlaceRequest();
        request.setMarketPlaceId(brno);
        mockMvc.perform(put("/users/me/marketplace")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_withOrders_returns409_withoutOrders_returns204() throws Exception {
        var used = createMarketPlace("Used");
        var unused = createMarketPlace("Unused");
        selectMarketPlace(token, used);
        placeOrder(token, "CARD");

        mockMvc.perform(delete("/marketplaces/" + used).with(bearer(token)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/marketplaces/" + unused).with(bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void marketPlaces_withoutToken_return401() throws Exception {
        mockMvc.perform(get("/marketplaces")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users/me/marketplace")).andExpect(status().isUnauthorized());
    }
}
