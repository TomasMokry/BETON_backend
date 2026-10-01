package org.tomo.beton.e2e;

import org.junit.jupiter.api.Test;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.support.AbstractE2ETest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityE2ETest extends AbstractE2ETest {

    @Test
    void adminEndpoint_asAdmin_returns200() throws Exception {
        var token = tokenFor("admin@mail.com", Role.ADMIN);

        mockMvc.perform(get("/admin/hello").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello Admin!"));
    }

    @Test
    void adminEndpoint_asUser_returns403() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);

        mockMvc.perform(get("/admin/hello").with(bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withTamperedToken_returns401() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        var tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get("/auth/me").with(bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealth_isPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
