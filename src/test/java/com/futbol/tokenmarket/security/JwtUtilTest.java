package com.futbol.tokenmarket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String SECRET = "clave-secreta-de-prueba-con-al-menos-256-bits-para-hmac-sha256";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ONE_HOUR_MS);
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("produce un token no vacío")
        void producesNonEmptyToken() {
            String token = jwtUtil.generateToken("jugador99");

            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("tokens distintos para usuarios distintos")
        void producesDifferentTokensForDifferentUsers() {
            String token1 = jwtUtil.generateToken("usuario1");
            String token2 = jwtUtil.generateToken("usuario2");

            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("recupera el username original del token")
        void recoversOriginalUsername() {
            String token = jwtUtil.generateToken("jugador99");

            String username = jwtUtil.extractUsername(token);

            assertThat(username).isEqualTo("jugador99");
        }

        @Test
        @DisplayName("funciona con usernames que contienen caracteres especiales")
        void worksWithSpecialCharacters() {
            String token = jwtUtil.generateToken("user@dominio.com");

            assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@dominio.com");
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("devuelve true para un token recién generado")
        void returnsTrueForFreshToken() {
            String token = jwtUtil.generateToken("jugador99");

            assertThat(jwtUtil.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("devuelve false para un token manipulado")
        void returnsFalseForTamperedToken() {
            String token = jwtUtil.generateToken("jugador99");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";

            assertThat(jwtUtil.isValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("devuelve false para un token firmado con otro secreto")
        void returnsFalseForTokenSignedWithDifferentSecret() {
            JwtUtil otherJwt = new JwtUtil("otro-secreto-completamente-diferente-y-de-256-bits-ok", ONE_HOUR_MS);
            String foreignToken = otherJwt.generateToken("jugador99");

            assertThat(jwtUtil.isValid(foreignToken)).isFalse();
        }

        @Test
        @DisplayName("devuelve false para un token expirado")
        void returnsFalseForExpiredToken() {
            JwtUtil instantExpiry = new JwtUtil(SECRET, -1L);
            String expiredToken = instantExpiry.generateToken("jugador99");

            assertThat(jwtUtil.isValid(expiredToken)).isFalse();
        }

        @Test
        @DisplayName("devuelve false para un string que no es un JWT")
        void returnsFalseForArbitraryString() {
            assertThat(jwtUtil.isValid("esto.no.es.un.jwt")).isFalse();
        }
    }
}
