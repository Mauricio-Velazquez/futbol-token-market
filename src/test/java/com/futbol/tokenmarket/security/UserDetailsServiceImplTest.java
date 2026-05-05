package com.futbol.tokenmarket.security;

import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository);
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("devuelve UserDetails con el username correcto cuando el usuario existe")
        void returnsUserDetailsWhenUserExists() throws IOException {
            when(userRepository.findByUsername("jugador99"))
                .thenReturn(Optional.of(new User("1", "jugador99", "hashed-password")));

            UserDetails result = service.loadUserByUsername("jugador99");

            assertThat(result.getUsername()).isEqualTo("jugador99");
        }

        @Test
        @DisplayName("devuelve UserDetails con la contraseña hasheada tal como está guardada")
        void returnsUserDetailsWithStoredPassword() throws IOException {
            when(userRepository.findByUsername("jugador99"))
                .thenReturn(Optional.of(new User("1", "jugador99", "$2a$hashed")));

            UserDetails result = service.loadUserByUsername("jugador99");

            assertThat(result.getPassword()).isEqualTo("$2a$hashed");
        }

        @Test
        @DisplayName("asigna el rol USER al usuario cargado")
        void assignsUserRole() throws IOException {
            when(userRepository.findByUsername("jugador99"))
                .thenReturn(Optional.of(new User("1", "jugador99", "hashed")));

            UserDetails result = service.loadUserByUsername("jugador99");

            assertThat(result.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("lanza UsernameNotFoundException cuando el usuario no existe")
        void throwsUsernameNotFoundWhenUserDoesNotExist() throws IOException {
            when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("convierte IOException del repositorio en UsernameNotFoundException")
        void wrapsIOExceptionAsUsernameNotFoundException() throws IOException {
            when(userRepository.findByUsername("jugador99")).thenThrow(new IOException("disco lleno"));

            assertThatThrownBy(() -> service.loadUserByUsername("jugador99"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("jugador99");
        }
    }
}
