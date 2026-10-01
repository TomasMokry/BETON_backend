package org.tomo.beton.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tomo.beton.config.JwtConfig;
import org.tomo.beton.dtos.Role;
import org.tomo.beton.entities.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtConfig jwtConfig;
    private JwtService jwtService;

    private final User user = User.builder()
            .id(7L).name("Tom").email("tom@mail.com").role(Role.ADMIN).build();

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("unit-test-secret-key-that-is-at-least-32-bytes");
        jwtConfig.setAccessTokenExpiration(900);
        jwtConfig.setRefreshTokenExpiration(604800);
        jwtService = new JwtService(jwtConfig);
    }

    @Test
    void accessToken_roundTrip_keepsUserIdAndRole() {
        var token = jwtService.generateAccessToken(user).toString();

        var jwt = jwtService.parseToken(token);

        assertThat(jwt).isNotNull();
        assertThat(jwt.getUserId()).isEqualTo(7L);
        assertThat(jwt.getRole()).isEqualTo(Role.ADMIN);
        assertThat(jwt.isExpired()).isFalse();
    }

    @Test
    void refreshToken_roundTrip_keepsUserId() {
        var token = jwtService.generateRefreshToken(user).toString();

        var jwt = jwtService.parseToken(token);

        assertThat(jwt).isNotNull();
        assertThat(jwt.getUserId()).isEqualTo(7L);
    }

    @Test
    void expiredToken_isRejected() {
        jwtConfig.setAccessTokenExpiration(-60);
        var token = jwtService.generateAccessToken(user).toString();

        // jjwt refuses expired tokens while parsing, so parseToken returns null
        assertThat(jwtService.parseToken(token)).isNull();
    }

    @Test
    void tokenSignedWithOtherKey_isRejected() {
        var otherConfig = new JwtConfig();
        otherConfig.setSecret("a-completely-different-secret-key-of-32-bytes");
        otherConfig.setAccessTokenExpiration(900);
        var foreignToken = new JwtService(otherConfig).generateAccessToken(user).toString();

        assertThat(jwtService.parseToken(foreignToken)).isNull();
    }

    @Test
    void tamperedToken_isRejected() {
        var token = jwtService.generateAccessToken(user).toString();
        var tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThat(jwtService.parseToken(tampered)).isNull();
    }

    @Test
    void garbageToken_isRejected() {
        assertThat(jwtService.parseToken("not-a-jwt")).isNull();
    }
}
