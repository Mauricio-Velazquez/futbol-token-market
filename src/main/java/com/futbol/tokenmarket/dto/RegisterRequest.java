package com.futbol.tokenmarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para registrar un nuevo usuario")
public class RegisterRequest {

    @JsonProperty("username")
    @NotBlank(message = "El nombre de usuario es requerido")
    @Size(min = 3, max = 20, message = "El nombre de usuario debe tener entre 3 y 20 caracteres")
    @Schema(description = "Nombre de usuario único", example = "usuario123", minLength = 3, maxLength = 20)
    private String username;

    @JsonProperty("password")
    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, max = 40, message = "La contraseña debe tener entre 6 y 40 caracteres")
    @Schema(description = "Contraseña del usuario", example = "password123", minLength = 6, maxLength = 40)
    private String password;

    @JsonProperty("passwordConfirm")
    @NotBlank(message = "La confirmación de contraseña es requerida")
    @Schema(description = "Confirmación de la contraseña (debe coincidir con password)", example = "password123")
    private String passwordConfirm;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String passwordConfirm) {
        this.username = username;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
