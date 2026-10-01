package org.tomo.beton.e2e;

import org.junit.jupiter.api.Test;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.support.AbstractE2ETest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryE2ETest extends AbstractE2ETest {

    @Test
    void getCategories_sortedByName() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        createCategory("Vases");
        createCategory("Bowls");
        createCategory("Candles");

        mockMvc.perform(get("/categories").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Bowls"))
                .andExpect(jsonPath("$[1].name").value("Candles"))
                .andExpect(jsonPath("$[2].name").value("Vases"))
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void getCategories_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isUnauthorized());
    }
}
