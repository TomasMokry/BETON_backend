package org.tomo.beton.e2e;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.AddItemToCartRequest;
import org.tomo.beton.dtos.UpdateCartItemRequest;
import org.tomo.beton.dtos.UpdateDiscountRequest;
import org.tomo.beton.entities.Category;
import org.tomo.beton.entities.Product;
import org.tomo.beton.support.AbstractE2ETest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cart endpoints are public, no token needed. */
class CartE2ETest extends AbstractE2ETest {

    private Category category;
    private Product vase;
    private Product bowl;

    @BeforeEach
    void setUp() {
        category = createCategory("Mixed");
        vase = createProduct("Vase", category, "100.00", 3);
        bowl = createProduct("Bowl", category, "25.50", 10);
    }

    private String createCart() throws Exception {
        var response = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
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

    private String quantity(int quantity) throws Exception {
        var request = new UpdateCartItemRequest();
        request.setQuantity(quantity);
        return json(request);
    }

    private String discount(Integer percent) throws Exception {
        var request = new UpdateDiscountRequest();
        request.setDiscountPercent(percent);
        return json(request);
    }

    @Test
    void createCart_returns201WithEmptyCart() throws Exception {
        var response = mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0))
                .andReturn().getResponse();

        String id = JsonPath.read(response.getContentAsString(), "$.id");
        assertThat(response.getHeader(HttpHeaders.LOCATION)).endsWith("/carts/" + id);
        assertThat(cartRepository.existsById(UUID.fromString(id))).isTrue();
    }

    @Test
    void addItem_newProduct_returns201WithItem() throws Exception {
        var cartId = createCart();
        var request = new AddItemToCartRequest();
        request.setProductId(vase.getId());

        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.product.id").value(vase.getId()))
                .andExpect(jsonPath("$.product.name").value("Vase"))
                .andExpect(jsonPath("$.product.stock").value(3))
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.totalPrice").value(100.00));
    }

    @Test
    void addItem_sameProductTwice_incrementsQuantity() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, vase.getId());

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(200.00));
    }

    @Test
    void addItem_unknownProduct_returns400() throws Exception {
        var cartId = createCart();
        var request = new AddItemToCartRequest();
        request.setProductId(999L);

        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product not found."));
    }

    @Test
    void addItem_beyondStock_returns400() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, vase.getId());
        addItem(cartId, vase.getId());
        var request = new AddItemToCartRequest();
        request.setProductId(vase.getId());

        mockMvc.perform(post("/carts/" + cartId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product out of stock."));
    }

    @Test
    void addItem_unknownCart_returns404() throws Exception {
        var request = new AddItemToCartRequest();
        request.setProductId(vase.getId());

        mockMvc.perform(post("/carts/" + UUID.randomUUID() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cart not found."));
    }

    @Test
    void getCart_returnsItemsSortedByNameWithTotal() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, bowl.getId());

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].product.name").value("Bowl"))
                .andExpect(jsonPath("$.items[1].product.name").value("Vase"))
                .andExpect(jsonPath("$.totalPrice").value(125.50));
    }

    @Test
    void getCart_unknownCart_returns404() throws Exception {
        mockMvc.perform(get("/carts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cart not found."));
    }

    @Test
    void updateItem_setsQuantity() throws Exception {
        var cartId = createCart();
        addItem(cartId, bowl.getId());

        mockMvc.perform(put("/carts/" + cartId + "/items/" + bowl.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantity(4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.totalPrice").value(102.00));

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items[0].quantity").value(4));
    }

    @Test
    void updateItem_zeroQuantity_returns400() throws Exception {
        var cartId = createCart();
        addItem(cartId, bowl.getId());

        mockMvc.perform(put("/carts/" + cartId + "/items/" + bowl.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantity(0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").value("Quantity must be greater than zero."));
    }

    @Test
    void updateItem_aboveStock_returns400() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());

        mockMvc.perform(put("/carts/" + cartId + "/items/" + vase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantity(4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product out of stock."));
    }

    @Test
    void updateItem_productNotInCart_returns400() throws Exception {
        var cartId = createCart();

        mockMvc.perform(put("/carts/" + cartId + "/items/" + vase.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quantity(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product not found."));
    }

    @Test
    void removeItem_removesOnlyThatProduct() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, bowl.getId());

        mockMvc.perform(delete("/carts/" + cartId + "/items/" + vase.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].product.name").value("Bowl"));
    }

    @Test
    void clearCart_removesAllItems() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, bowl.getId());

        mockMvc.perform(delete("/carts/" + cartId + "/items"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0));
    }

    @Test
    void clearCart_unknownCart_returns404() throws Exception {
        mockMvc.perform(delete("/carts/" + UUID.randomUUID() + "/items"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItemDiscount_reducesItemTotalAndIsPersisted() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());
        addItem(cartId, bowl.getId());

        mockMvc.perform(put("/carts/" + cartId + "/items/" + vase.getId() + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercent").value(10))
                .andExpect(jsonPath("$.subtotalPrice").value(100.00))
                .andExpect(jsonPath("$.totalPrice").value(90.00));

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.items[1].product.name").value("Vase"))
                .andExpect(jsonPath("$.items[1].discountPercent").value(10))
                .andExpect(jsonPath("$.totalPrice").value(115.50));
    }

    @Test
    void updateCartDiscount_appliesToTotalAndIsPersisted() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());

        mockMvc.perform(put("/carts/" + cartId + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(25)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercent").value(25))
                .andExpect(jsonPath("$.subtotalPrice").value(100.00))
                .andExpect(jsonPath("$.discountAmount").value(25.00))
                .andExpect(jsonPath("$.totalPrice").value(75.00));

        mockMvc.perform(get("/carts/" + cartId))
                .andExpect(jsonPath("$.discountPercent").value(25))
                .andExpect(jsonPath("$.totalPrice").value(75.00));
    }

    @Test
    void updateCartDiscount_gift_makesTotalZero() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());

        mockMvc.perform(put("/carts/" + cartId + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(0.0));
    }

    @Test
    void updateDiscount_notAllowedValue_returns400() throws Exception {
        var cartId = createCart();
        addItem(cartId, vase.getId());

        mockMvc.perform(put("/carts/" + cartId + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(7)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid discount percent."));

        mockMvc.perform(put("/carts/" + cartId + "/items/" + vase.getId() + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid discount percent."));
    }

    @Test
    void updateDiscount_missingValue_returns400() throws Exception {
        var cartId = createCart();

        mockMvc.perform(put("/carts/" + cartId + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.discountPercent").value("Discount percent must be provided."));
    }

    @Test
    void updateItemDiscount_productNotInCart_returns400() throws Exception {
        var cartId = createCart();

        mockMvc.perform(put("/carts/" + cartId + "/items/" + vase.getId() + "/discount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(discount(10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product not found."));
    }
}
