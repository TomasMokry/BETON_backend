package org.tomo.beton.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.ChangePasswordRequest;
import org.tomo.beton.dtos.RegisterUserRequest;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.dtos.UpdateUserRequest;
import org.tomo.beton.support.AbstractE2ETest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserE2ETest extends AbstractE2ETest {

    private static RegisterUserRequest registerRequest(String name, String email, String password) {
        var request = new RegisterUserRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void register_validRequest_returns201AndStoresHashedPassword() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("Tom", "tom@mail.com", "secret1"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/users/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tom"))
                .andExpect(jsonPath("$.email").value("tom@mail.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var user = userRepository.findByEmail("tom@mail.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("secret1", user.getPassword())).isTrue();

        // The new account can log in
        loginAndGetToken("tom@mail.com", "secret1");
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        createUser("tom@mail.com", "secret1", Role.USER);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("Tom", "tom@mail.com", "secret1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already registered."));
    }

    @Test
    void register_invalidFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registerRequest("", "Tom@Mail.com", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name is required"))
                .andExpect(jsonPath("$.email").value("Email must be in lowercase"))
                .andExpect(jsonPath("$.password").exists());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void getAllUsers_sortedByEmail() throws Exception {
        var token = tokenFor("b@mail.com", Role.USER);
        createUser("a@mail.com", "secret1", Role.USER);
        createUser("c@mail.com", "secret1", Role.USER);

        mockMvc.perform(get("/users").param("sort", "email").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].email").value("a@mail.com"))
                .andExpect(jsonPath("$[1].email").value("b@mail.com"))
                .andExpect(jsonPath("$[2].email").value("c@mail.com"));
    }

    @Test
    void getAllUsers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_existing_returnsUser() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);

        mockMvc.perform(get("/users/1").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("tom@mail.com"));
    }

    @Test
    void getUser_missing_returns404() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);

        mockMvc.perform(get("/users/999").with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_changesNameAndEmail() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        var request = new UpdateUserRequest();
        request.setName("Tomas");
        request.setEmail("tomas@mail.com");

        mockMvc.perform(put("/users/1")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tomas"))
                .andExpect(jsonPath("$.email").value("tomas@mail.com"));

        assertThat(userRepository.findById(1L).orElseThrow().getName()).isEqualTo("Tomas");
    }

    @Test
    void updateUser_missing_returns404() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);

        mockMvc.perform(put("/users/999")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UpdateUserRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_removesUser() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        var other = createUser("other@mail.com", "secret1", Role.USER);

        mockMvc.perform(delete("/users/" + other.getId()).with(bearer(token)))
                .andExpect(status().isOk());

        assertThat(userRepository.existsById(other.getId())).isFalse();
    }

    @Test
    void deleteUser_missing_returns404() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);

        mockMvc.perform(delete("/users/999").with(bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void changePassword_correctOldPassword_newPasswordWorksForLogin() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        var request = new ChangePasswordRequest();
        request.setOldPassword("password123");
        request.setNewPassword("brandNew456");

        mockMvc.perform(post("/users/1/change-password")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk());

        loginAndGetToken("tom@mail.com", "brandNew456");
        assertThat(userRepository.findById(1L).orElseThrow().getPassword()).isNotEqualTo("brandNew456");
    }

    @Test
    void changePassword_wrongOldPassword_returns401AndKeepsPassword() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        var request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("brandNew456");

        mockMvc.perform(post("/users/1/change-password")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isUnauthorized());

        loginAndGetToken("tom@mail.com", "password123");
    }

    @Test
    void changePassword_missingUser_returns404() throws Exception {
        var token = tokenFor("tom@mail.com", Role.USER);
        var request = new ChangePasswordRequest();
        request.setOldPassword("password123");
        request.setNewPassword("brandNew456");

        mockMvc.perform(post("/users/999/change-password")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound());
    }
}
