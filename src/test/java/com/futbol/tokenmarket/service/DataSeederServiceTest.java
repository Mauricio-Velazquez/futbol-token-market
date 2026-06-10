package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.UserRepository;
import com.futbol.tokenmarket.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("DataSeederService")
class DataSeederServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PlayerRepository playerRepository;
    @Mock private OrderService orderService;
    @Mock private QuoteService quoteService;

    private DataSeederService dataSeederService;

    @BeforeEach
    void setUp() {
        dataSeederService = new DataSeederService(
                userRepository, walletRepository, passwordEncoder, playerRepository, orderService, quoteService
        );
    }

    @Test
    @DisplayName("retorna 0 y no ejecuta si la base de datos de jugadores está vacía")
    void returnsZeroWhenNoPlayersExist() {
        when(playerRepository.findAll()).thenReturn(List.of());

        int result = dataSeederService.seed();

        assertThat(result).isZero();
        verifyNoInteractions(userRepository, orderService);
    }

    @Test
    @DisplayName("ejecuta el seed completo barriendo todos los perfiles de simulación y excepciones")
    void executesFullSeedSuccessfully() throws Exception {
        // 1. Armamos una lista con un par de jugadores para simular mercado
        Player p1 = new Player(); p1.setId("p1"); p1.setName("Lionel Messi");
        Player p2 = new Player(); p2.setId("p2"); p2.setName("Cristiano Ronaldo");
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2));

        // 2. Simulamos que algunos usuarios ya existen y otros no para pisar ambas ramas del 'continue'
        // 'buildSeeds()' expone primero "seed_acum_01", simulamos que ese ya existe
        when(userRepository.findByUsername("seed_acum_01")).thenReturn(Optional.of(new User()));
        // Los demás darán empty por defecto con Mockito, obligando a crearlos

        // 3. Mockeamos PasswordEncoder y QuoteService con precios fijos accesibles
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(quoteService.getLatestPriceForPlayer(anyString())).thenReturn(BigDecimal.valueOf(50.0));

        // 4. Forzamos que una de las compras tire excepción para cubrir el bloque 'catch' del seeder
        // Hacemos que cuando intente comprar para "seed_acum_02" falle, y las demás pasen limpio
        doThrow(new RuntimeException("Simulated Market Error"))
                .when(orderService).buy(eq("seed_acum_02"), any());

        // 5. Ejecutamos la siembra de datos
        int createdUsers = dataSeederService.seed();

        // 6. Verificaciones de control
        // El total de semillas en tu mapa es 37. Como saltamos 1 que ya existía, debe crear 36.
        assertThat(createdUsers).isEqualTo(36);

        // Verificamos que se haya guardado en los repositorios correspondientes
        verify(userRepository, atLeastOnce()).save(any());
        verify(walletRepository, atLeastOnce()).save(any());
        
        // Verificamos que se haya llamado al motor de compras del OrderService
        verify(orderService, atLeastOnce()).buy(anyString(), any());
    }
}
