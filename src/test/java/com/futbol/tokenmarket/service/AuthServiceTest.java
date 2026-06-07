package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.dto.AuthResponse;
import com.futbol.tokenmarket.dto.LoginRequest;
import com.futbol.tokenmarket.dto.RegisterRequest;
import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.repository.UserRepository;
import com.futbol.tokenmarket.repository.WalletRepository;
import com.futbol.tokenmarket.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, walletRepository, passwordEncoder, authenticationManager, jwtUtil);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("devuelve un AuthResponse con token y username cuando el registro es exitoso")
        void returnsAuthResponseWithTokenAndUsername() {
            RegisterRequest request = new RegisterRequest("jugador99", "secret123", "secret123");
            when(userRepository.findByUsername("jugador99")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken("jugador99")).thenReturn("jwt-token-generado");

            AuthResponse response = authService.register(request);

            assertThat(response.getToken()).isEqualTo("jwt-token-generado");
            assertThat(response.getUsername()).isEqualTo("jugador99");
        }

        @Test
        @DisplayName("lanza excepción cuando las contraseñas no coinciden")
        void throwsExceptionWhenPasswordsDoNotMatch() {
            RegisterRequest request = new RegisterRequest("jugador99", "secret123", "otraPassword");

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Las contraseñas no coinciden");
        }

        @Test
        @DisplayName("lanza excepción cuando el nombre de usuario ya está registrado")
        void throwsExceptionWhenUsernameAlreadyTaken() {
            RegisterRequest request = new RegisterRequest("jugador99", "secret123", "secret123");
            when(userRepository.findByUsername("jugador99"))
                .thenReturn(Optional.of(new User("1", "jugador99", "hash")));

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El nombre de usuario ya está en uso");
        }

        @Test
        @DisplayName("encripta la contraseña antes de guardar el usuario")
        void encodesPasswordBeforePersisting() {
            RegisterRequest request = new RegisterRequest("jugador99", "secret123", "secret123");
            when(userRepository.findByUsername("jugador99")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
            when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken(anyString())).thenReturn("token");

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("hashed-password");
        }

        @Test
        @DisplayName("asigna un ID nuevo al usuario registrado")
        void assignsNewIdToRegisteredUser() {
            RegisterRequest request = new RegisterRequest("jugador99", "secret123", "secret123");
            when(userRepository.findByUsername("jugador99")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(jwtUtil.generateToken(anyString())).thenReturn("token");

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isNotNull().isNotBlank();
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("devuelve un AuthResponse con token cuando las credenciales son válidas")
        void returnsAuthResponseWhenCredentialsAreValid() {
            LoginRequest request = new LoginRequest("jugador99", "secret123");
            when(jwtUtil.generateToken("jugador99")).thenReturn("jwt-valido");

            AuthResponse response = authService.login(request);

            assertThat(response.getToken()).isEqualTo("jwt-valido");
            assertThat(response.getUsername()).isEqualTo("jugador99");
        }

        @Test
        @DisplayName("delega la autenticación al AuthenticationManager")
        void delegatesAuthenticationToAuthenticationManager() {
            LoginRequest request = new LoginRequest("jugador99", "secret123");
            when(jwtUtil.generateToken(anyString())).thenReturn("token");

            authService.login(request);

            verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("jugador99", "secret123")
            );
        }

        @Test
        @DisplayName("lanza excepción cuando las credenciales son incorrectas")
        void throwsExceptionWhenCredentialsAreInvalid() {
            LoginRequest request = new LoginRequest("jugador99", "wrongPassword");
            doThrow(new BadCredentialsException("bad credentials"))
                .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario o contraseña incorrectos");
        }
    }
}
