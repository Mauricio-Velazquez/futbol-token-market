package com.futbol.tokenmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response con información de autenticación")
public class AuthResponse {

    @JsonProperty("token")
    @Schema(description = "Token JWT para autenticación", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @JsonProperty("username")
    @Schema(description = "Nombre de usuario autenticado", example = "usuario123")
    private String username;

    @JsonProperty("message")
    @Schema(description = "Mensaje de respuesta", example = "Autenticación exitosa")
    private String message;

    public AuthResponse() {}

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
        this.message = "Autenticación exitosa";
    }

    public AuthResponse(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
