package org.tomo.beton.e2e;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.tomo.beton.dtos.LoginRequest;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.support.AbstractE2ETest;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthE2ETest extends AbstractE2ETest {

    private static final String EMAIL = "tom@mail.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        createUser(EMAIL, PASSWORD, Role.USER);
    }

    private String loginBody(String email, String password) throws Exception {
        var request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return json(request);
    }

    private String refreshTokenFromLogin() throws Exception {
        var setCookie = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookie.substring("refreshToken=".length(), setCookie.indexOf(';'));
    }

    @Test
    void login_validCredentials_returnsAccessTokenAndRefreshCookie() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        startsWith("refreshToken="),
                        containsString("Path=/auth/refresh"),
                        containsString("HttpOnly"),
                        containsString("Secure"))));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@mail.com", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_invalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("not-an-email", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").value("Password is required"));
    }

    @Test
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body"));
    }

    @Test
    void me_withToken_returnsCurrentUser() throws Exception {
        var token = loginAndGetToken(EMAIL, PASSWORD);

        mockMvc.perform(get("/auth/me").with(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/auth/me").with(bearer("garbage")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validCookie_returnsNewAccessToken() throws Exception {
        var refreshToken = refreshTokenFromLogin();

        var response = mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andReturn().getResponse().getContentAsString();

        // The new access token is usable
        String accessToken = JsonPath.read(response, "$.token");
        mockMvc.perform(get("/auth/me").with(bearer(accessToken)))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_invalidCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh").cookie(new Cookie("refreshToken", "garbage")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_missingCookie_returns400() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_clearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        startsWith("refreshToken=;"),
                        containsString("Max-Age=0"),
                        containsString("Path=/auth/refresh"))));
    }
}
