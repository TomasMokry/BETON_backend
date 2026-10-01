package org.tomo.beton.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.ProductDto;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.entities.Category;
import org.tomo.beton.support.AbstractE2ETest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductE2ETest extends AbstractE2ETest {

    private String token;
    private Category vases;
    private Category bowls;

    @BeforeEach
    void setUp() throws Exception {
        token = tokenFor("tom@mail.com", Role.USER);
        vases = createCategory("Vases");
        bowls = createCategory("Bowls");
    }

    private ProductDto productDto(Byte categoryId) {
        var dto = new ProductDto();
        dto.setName("Vase Aurora");
        dto.setSize("L");
        dto.setPrice(new BigDecimal("199.90"));
        dto.setDescription("A tall concrete vase");
        dto.setHeight(30);
        dto.setWidth(12);
        dto.setWeight(900);
        dto.setLength(12);
        dto.setColor("Grey");
        dto.setUrlImage("/images/aurora.jpg");
        dto.setAmount(4);
        dto.setCategoryId(categoryId);
        return dto;
    }

    @Test
    void getProducts_all_sortedByName() throws Exception {
        createProduct("Vase B", vases, "10", 1);
        createProduct("Bowl A", bowls, "10", 1);
        createProduct("Vase A", vases, "10", 1);

        mockMvc.perform(get("/products").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Bowl A"))
                .andExpect(jsonPath("$[1].name").value("Vase A"))
                .andExpect(jsonPath("$[2].name").value("Vase B"));
    }

    @Test
    void getProducts_filteredByCategory() throws Exception {
        createProduct("Vase B", vases, "10", 1);
        createProduct("Bowl A", bowls, "10", 1);
        createProduct("Vase A", vases, "10", 1);

        mockMvc.perform(get("/products").param("categoryId", vases.getId().toString()).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Vase A"))
                .andExpect(jsonPath("$[1].name").value("Vase B"))
                .andExpect(jsonPath("$[0].categoryId").value(vases.getId().intValue()));
    }

    @Test
    void getProducts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProduct_existing_returnsProduct() throws Exception {
        var product = createProduct("Vase A", vases, "49.50", 3);

        mockMvc.perform(get("/products/" + product.getId()).with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Vase A"))
                .andExpect(jsonPath("$.price").value(49.50))
                .andExpect(jsonPath("$.amount").value(3))
                .andExpect(jsonPath("$.categoryId").value(vases.getId().intValue()));
    }

    @Test
    void getProduct_missing_returns400() throws Exception {
        mockMvc.perform(get("/products/999").with(bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createProduct_valid_returns201AndPersists() throws Exception {
        mockMvc.perform(post("/products")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productDto(vases.getId()))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/products/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Vase Aurora"))
                .andExpect(jsonPath("$.categoryId").value(vases.getId().intValue()));

        var saved = productRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getCategory().getId()).isEqualTo(vases.getId());
    }

    @Test
    void createProduct_unknownCategory_returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productDto((byte) 99))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category not found."));

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void createProduct_invalidFields_returns400WithFieldErrors() throws Exception {
        var dto = productDto(vases.getId());
        dto.setName(" ");
        dto.setPrice(new BigDecimal("-1"));
        dto.setAmount(0);

        mockMvc.perform(post("/products")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.price").exists())
                .andExpect(jsonPath("$.amount").exists());

        assertThat(productRepository.count()).isZero();
    }

    @Test
    void updateProduct_changesFieldsAndCategory() throws Exception {
        var product = createProduct("Vase A", vases, "10", 1);
        var dto = productDto(bowls.getId());
        dto.setName("Bowl Renamed");

        mockMvc.perform(put("/products/" + product.getId())
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Bowl Renamed"))
                .andExpect(jsonPath("$.categoryId").value(bowls.getId().intValue()));

        var updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Bowl Renamed");
        assertThat(updated.getCategory().getId()).isEqualTo(bowls.getId());
    }

    @Test
    void updateProduct_unknownCategory_returns400CategoryNotFound() throws Exception {
        var product = createProduct("Vase A", vases, "10", 1);

        mockMvc.perform(put("/products/" + product.getId())
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productDto((byte) 99))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category not found."));
    }

    @Test
    void updateProduct_missingProduct_returns400() throws Exception {
        mockMvc.perform(put("/products/999")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productDto(vases.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProduct_removesProduct() throws Exception {
        var product = createProduct("Vase A", vases, "10", 1);

        mockMvc.perform(delete("/products/" + product.getId()).with(bearer(token)))
                .andExpect(status().isOk());

        assertThat(productRepository.existsById(product.getId())).isFalse();
    }

    @Test
    void deleteProduct_missing_returns400() throws Exception {
        mockMvc.perform(delete("/products/999").with(bearer(token)))
                .andExpect(status().isBadRequest());
    }
}
