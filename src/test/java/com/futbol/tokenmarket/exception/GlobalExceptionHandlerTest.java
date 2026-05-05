package com.futbol.tokenmarket.exception;

import com.futbol.tokenmarket.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(WebRequest.class);
    }

    @Nested
    @DisplayName("IllegalArgumentException")
    class IllegalArgument {

        @Test
        @DisplayName("responde con 400 Bad Request")
        void returns400() {
            ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("dato inválido"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("incluye el mensaje de la excepción en el body")
        void includesExceptionMessageInBody() {
            ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("dato inválido"), request);

            assertThat(response.getBody().getMessage()).isEqualTo("dato inválido");
        }
    }

    @Nested
    @DisplayName("BadCredentialsException")
    class BadCredentials {

        @Test
        @DisplayName("responde con 401 Unauthorized")
        void returns401() {
            ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentialsException(new BadCredentialsException("bad"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("no expone detalles internos de la credencial")
        void doesNotExposeCredentialDetails() {
            ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentialsException(new BadCredentialsException("hash interno"), request);

            assertThat(response.getBody().getMessage()).doesNotContain("hash interno");
        }
    }

    @Nested
    @DisplayName("UsernameNotFoundException")
    class UsernameNotFound {

        @Test
        @DisplayName("responde con 401 Unauthorized")
        void returns401() {
            ResponseEntity<ErrorResponse> response =
                handler.handleUsernameNotFoundException(new UsernameNotFoundException("no existe"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Exception genérica")
    class GenericException {

        @Test
        @DisplayName("responde con 500 Internal Server Error")
        void returns500() {
            ResponseEntity<ErrorResponse> response =
                handler.handleGlobalException(new RuntimeException("fallo inesperado"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("no filtra detalles internos del error al cliente")
        void doesNotLeakInternalDetails() {
            ResponseEntity<ErrorResponse> response =
                handler.handleGlobalException(new RuntimeException("NullPointerException en línea 42"), request);

            assertThat(response.getBody().getMessage())
                .doesNotContain("NullPointerException", "línea 42");
        }
    }
}
