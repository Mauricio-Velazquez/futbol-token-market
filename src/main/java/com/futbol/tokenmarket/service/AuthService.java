package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.dto.LoginRequest;
import com.futbol.tokenmarket.dto.RegisterRequest;
import com.futbol.tokenmarket.dto.AuthResponse;
import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.repository.UserRepository;
import com.futbol.tokenmarket.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      AuthenticationManager authenticationManager,
                      JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) throws IOException {
        // Validar que las contraseñas coincidan
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }

        // Verificar si el usuario ya existe
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("El nombre de usuario ya está en uso");
        }

        // Crear nuevo usuario
        User newUser = new User(
            UUID.randomUUID().toString(),
            request.getUsername(),
            passwordEncoder.encode(request.getPassword())
        );

        // Guardar usuario
        userRepository.save(newUser);

        // Generar token
        String token = jwtUtil.generateToken(request.getUsername());

        return new AuthResponse(token, request.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Autenticar
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // Generar token
            String token = jwtUtil.generateToken(request.getUsername());

            return new AuthResponse(token, request.getUsername());
        } catch (AuthenticationException e) {
            throw new IllegalArgumentException("Usuario o contraseña incorrectos");
        }
    }
}
