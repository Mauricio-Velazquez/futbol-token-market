package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.dto.AuthResponse;
import com.futbol.tokenmarket.dto.LoginRequest;
import com.futbol.tokenmarket.dto.RegisterRequest;
import com.futbol.tokenmarket.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de autenticacion")
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private Authentication authentication;
    @Mock private HttpServletRequest request;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("devuelve 201 y delega el registro al servicio")
        void returnsCreatedAndDelegatesToService() throws Exception {
            RegisterRequest registerRequest = new RegisterRequest("usuario1", "secret123", "secret123");
            AuthResponse response = new AuthResponse("token-registro", "usuario1");
            when(authService.register(registerRequest)).thenReturn(response);

            var result = authController.register(registerRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isEqualTo(response);
            verify(authService).register(registerRequest);
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("devuelve 200 y delega el login al servicio")
        void returnsOkAndDelegatesToService() {
            LoginRequest loginRequest = new LoginRequest("usuario1", "secret123");
            AuthResponse response = new AuthResponse("token-login", "usuario1");
            when(authService.login(loginRequest)).thenReturn(response);

            var result = authController.login(loginRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(response);
            verify(authService).login(loginRequest);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("extrae el token Bearer y responde con el usuario")
        void extractsBearerTokenAndReturnsUsername() {
            when(authentication.getName()).thenReturn("usuario1");
            when(request.getHeader("Authorization")).thenReturn("Bearer token-123");

            var result = authController.validateToken(authentication, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getUsername()).isEqualTo("usuario1");
            assertThat(result.getBody().getToken()).isEqualTo("token-123");
            assertThat(result.getBody().getMessage()).contains("usuario1");
        }

        @Test
        @DisplayName("devuelve token nulo cuando el header no tiene Bearer")
        void returnsNullTokenWhenHeaderDoesNotContainBearer() {
            when(authentication.getName()).thenReturn("usuario1");
            when(request.getHeader("Authorization")).thenReturn("Basic abc");

            var result = authController.validateToken(authentication, request);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getToken()).isNull();
        }
    }
}