package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.dto.OrderRequest;
import com.futbol.tokenmarket.dto.OrderResponse;
import com.futbol.tokenmarket.dto.PortfolioEntryResponse;
import com.futbol.tokenmarket.dto.TransactionResponse;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.TokenHolding;
import com.futbol.tokenmarket.model.Transaction;
import com.futbol.tokenmarket.model.TransactionType;
import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.model.Wallet;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TokenHoldingRepository;
import com.futbol.tokenmarket.repository.TransactionRepository;
import com.futbol.tokenmarket.repository.UserRepository;
import com.futbol.tokenmarket.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("OrderService")
class OrderServiceTest {

    private static final String USERNAME = "jugador1";
    private static final String PLAYER_ID = "player-1";
    private static final String PLAYER_NAME = "Lionel Messi";
    private static final String SISTEMA = SystemInitializationService.SISTEMA_USERNAME;

    @Mock private UserRepository userRepository;
    @Mock private PlayerRepository playerRepository;
    @Mock private TokenHoldingRepository tokenHoldingRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private QuoteService quoteService;

    private OrderService orderService;

    private User user;
    private User sistema;
    private Player player;
    private Wallet userWallet;
    private Wallet sistemaWallet;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(userRepository, playerRepository,
                tokenHoldingRepository, walletRepository, transactionRepository, quoteService,
                new SimpleMeterRegistry());

        user = new User("user-id", USERNAME, "hash");
        sistema = new User("sistema-id", SISTEMA, "hash");
        player = new Player();
        player.setId(PLAYER_ID);
        player.setName(PLAYER_NAME);
        userWallet = new Wallet(user);
        userWallet.setBalance(BigDecimal.valueOf(1000));
        sistemaWallet = new Wallet(sistema);
        sistemaWallet.setBalance(BigDecimal.valueOf(5000));
    }

    @Nested
    @DisplayName("buy")
    class Buy {

        @Test
        @DisplayName("devuelve un OrderResponse con type BUY y datos correctos")
        void returnsOrderResponseWithCorrectData() {
            stubHappyPathBuy(10, BigDecimal.valueOf(50.0), 20);

            OrderResponse response = orderService.buy(USERNAME, requestOf(PLAYER_ID, 10));

            assertThat(response.getType()).isEqualTo("BUY");
            assertThat(response.getPlayerId()).isEqualTo(PLAYER_ID);
            assertThat(response.getPlayerName()).isEqualTo(PLAYER_NAME);
            assertThat(response.getQuantity()).isEqualTo(10);
            assertThat(response.getPricePerToken()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500.0));
        }

        @Test
        @DisplayName("el balance restante de la wallet refleja el costo total descontado")
        void walletBalanceAfterReflectsDeductedCost() {
            stubHappyPathBuy(5, BigDecimal.valueOf(100.0), 10);

            OrderResponse response = orderService.buy(USERNAME, requestOf(PLAYER_ID, 5));

            assertThat(response.getWalletBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(500.0));
        }

        @Test
        @DisplayName("lanza excepción cuando la cantidad es cero")
        void throwsExceptionWhenQuantityIsZero() {
            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La cantidad debe ser mayor a 0");
        }

        @Test
        @DisplayName("lanza excepción cuando la cantidad es negativa")
        void throwsExceptionWhenQuantityIsNegative() {
            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, -1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La cantidad debe ser mayor a 0");
        }

        @Test
        @DisplayName("lanza excepción cuando el usuario no existe")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(USERNAME);
        }

        @Test
        @DisplayName("lanza excepción cuando el jugador no existe")
        void throwsExceptionWhenPlayerNotFound() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(PLAYER_ID);
        }

        @Test
        @DisplayName("lanza excepción cuando el sistema no tiene holding del jugador")
        void throwsExceptionWhenSistemaHasNoHolding() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(50.0));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, sistema)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, 1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No hay tokens disponibles para este jugador");
        }

        @Test
        @DisplayName("lanza excepción cuando el sistema no tiene suficientes tokens")
        void throwsExceptionWhenSistemaHasInsufficientTokens() {
            TokenHolding sistemaHolding = new TokenHolding(player, sistema, 3);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(50.0));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, sistema)).thenReturn(Optional.of(sistemaHolding));

            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, 10)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suficientes tokens");
        }

        @Test
        @DisplayName("lanza excepción cuando el saldo de la wallet es insuficiente")
        void throwsExceptionWhenWalletBalanceIsInsufficient() {
            TokenHolding sistemaHolding = new TokenHolding(player, sistema, 20);
            userWallet.setBalance(BigDecimal.valueOf(10));
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(100.0));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, sistema)).thenReturn(Optional.of(sistemaHolding));
            when(walletRepository.findByUser(user)).thenReturn(Optional.of(userWallet));

            assertThatThrownBy(() -> orderService.buy(USERNAME, requestOf(PLAYER_ID, 5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Saldo insuficiente");
        }

        @Test
        @DisplayName("crea un nuevo holding para el usuario si no tenía tokens de ese jugador")
        void createsNewHoldingWhenUserHadNoTokens() {
            stubHappyPathBuy(5, BigDecimal.valueOf(50.0), 20);
            when(tokenHoldingRepository.findByPlayerAndOwner(player, user)).thenReturn(Optional.empty());

            orderService.buy(USERNAME, requestOf(PLAYER_ID, 5));

            ArgumentCaptor<TokenHolding> captor = ArgumentCaptor.forClass(TokenHolding.class);
            verify(tokenHoldingRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            TokenHolding userHolding = captor.getAllValues().stream()
                    .filter(h -> user.equals(h.getOwner()))
                    .findFirst().orElseThrow();
            assertThat(userHolding.getQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("incrementa el holding existente del usuario")
        void incrementsExistingUserHolding() {
            TokenHolding existing = new TokenHolding(player, user, 10);
            stubHappyPathBuy(5, BigDecimal.valueOf(50.0), 20);
            when(tokenHoldingRepository.findByPlayerAndOwner(player, user)).thenReturn(Optional.of(existing));

            orderService.buy(USERNAME, requestOf(PLAYER_ID, 5));

            ArgumentCaptor<TokenHolding> captor = ArgumentCaptor.forClass(TokenHolding.class);
            verify(tokenHoldingRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
            TokenHolding userHolding = captor.getAllValues().stream()
                    .filter(h -> user.equals(h.getOwner()))
                    .findFirst().orElseThrow();
            assertThat(userHolding.getQuantity()).isEqualTo(15);
        }

        @Test
        @DisplayName("guarda una transacción de tipo BUY")
        void savesTransactionOfTypeBuy() {
            stubHappyPathBuy(3, BigDecimal.valueOf(50.0), 20);

            orderService.buy(USERNAME, requestOf(PLAYER_ID, 3));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TransactionType.BUY);
            assertThat(captor.getValue().getQuantity()).isEqualTo(3);
            assertThat(captor.getValue().getUser()).isEqualTo(user);
        }

        private void stubHappyPathBuy(int qty, BigDecimal price, int sistemaQty) {
            TokenHolding sistemaHolding = new TokenHolding(player, sistema, sistemaQty);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(price);
            when(tokenHoldingRepository.findByPlayerAndOwner(player, sistema)).thenReturn(Optional.of(sistemaHolding));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, user)).thenReturn(Optional.empty());
            when(walletRepository.findByUser(user)).thenReturn(Optional.of(userWallet));
            when(walletRepository.findByUser(sistema)).thenReturn(Optional.of(sistemaWallet));
            when(tokenHoldingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }
    }

    @Nested
    @DisplayName("sell")
    class Sell {

        @Test
        @DisplayName("devuelve un OrderResponse con type SELL y datos correctos")
        void returnsOrderResponseWithCorrectData() {
            stubHappyPathSell(5, BigDecimal.valueOf(80.0), 10);

            OrderResponse response = orderService.sell(USERNAME, requestOf(PLAYER_ID, 5));

            assertThat(response.getType()).isEqualTo("SELL");
            assertThat(response.getPlayerId()).isEqualTo(PLAYER_ID);
            assertThat(response.getQuantity()).isEqualTo(5);
            assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(400.0));
        }

        @Test
        @DisplayName("el balance de la wallet aumenta con los fondos recibidos")
        void walletBalanceIncreasesByProceeds() {
            stubHappyPathSell(2, BigDecimal.valueOf(100.0), 5);

            OrderResponse response = orderService.sell(USERNAME, requestOf(PLAYER_ID, 2));

            assertThat(response.getWalletBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(1200.0));
        }

        @Test
        @DisplayName("lanza excepción cuando la cantidad es cero")
        void throwsExceptionWhenQuantityIsZero() {
            assertThatThrownBy(() -> orderService.sell(USERNAME, requestOf(PLAYER_ID, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La cantidad debe ser mayor a 0");
        }

        @Test
        @DisplayName("lanza excepción cuando el usuario no tiene tokens de ese jugador")
        void throwsExceptionWhenUserHasNoHolding() {
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(50.0));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, user)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.sell(USERNAME, requestOf(PLAYER_ID, 1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("No tenés tokens de este jugador");
        }

        @Test
        @DisplayName("lanza excepción cuando el usuario no tiene suficientes tokens")
        void throwsExceptionWhenUserHasInsufficientTokens() {
            TokenHolding userHolding = new TokenHolding(player, user, 2);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(50.0));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, user)).thenReturn(Optional.of(userHolding));

            assertThatThrownBy(() -> orderService.sell(USERNAME, requestOf(PLAYER_ID, 5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suficientes tokens");
        }

        @Test
        @DisplayName("guarda una transacción de tipo SELL")
        void savesTransactionOfTypeSell() {
            stubHappyPathSell(2, BigDecimal.valueOf(50.0), 10);

            orderService.sell(USERNAME, requestOf(PLAYER_ID, 2));

            ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(TransactionType.SELL);
            assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        }

        private void stubHappyPathSell(int qty, BigDecimal price, int userQty) {
            TokenHolding userHolding = new TokenHolding(player, user, userQty);
            when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
            when(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
            when(userRepository.findByUsername(SISTEMA)).thenReturn(Optional.of(sistema));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(price);
            when(tokenHoldingRepository.findByPlayerAndOwner(player, user)).thenReturn(Optional.of(userHolding));
            when(tokenHoldingRepository.findByPlayerAndOwner(player, sistema)).thenReturn(Optional.empty());
            when(walletRepository.findByUser(user)).thenReturn(Optional.of(userWallet));
            when(walletRepository.findByUser(sistema)).thenReturn(Optional.of(sistemaWallet));
            when(tokenHoldingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }
    }

    @Nested
    @DisplayName("getPortfolio")
    class GetPortfolio {

        @Test
        @DisplayName("devuelve solo los holdings con cantidad mayor a cero")
        void returnsOnlyHoldingsWithPositiveQuantity() {
            TokenHolding withTokens = new TokenHolding(player, user, 5);
            Player playerB = new Player();
            playerB.setId("player-2");
            playerB.setName("Cristiano Ronaldo");
            TokenHolding withoutTokens = new TokenHolding(playerB, user, 0);
            Transaction buyTx = new Transaction(user, player, TransactionType.BUY, 5,
                    BigDecimal.valueOf(50.0), java.time.LocalDateTime.now());
            when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
            when(tokenHoldingRepository.findByOwner(user)).thenReturn(List.of(withTokens, withoutTokens));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(50.0));
            when(transactionRepository.findByUserAndPlayerOrderByCreatedAtAsc(user, player)).thenReturn(List.of(buyTx));

            List<PortfolioEntryResponse> portfolio = orderService.getPortfolio("user-id");

            assertThat(portfolio).hasSize(1);
            assertThat(portfolio.get(0).getPlayerId()).isEqualTo(PLAYER_ID);
        }

        @Test
        @DisplayName("calcula precio promedio de compra correctamente")
        void computesCorrectAvgBuyPrice() {
            TokenHolding holding = new TokenHolding(player, user, 10);
            Transaction buy1 = new Transaction(user, player, TransactionType.BUY, 5,
                    BigDecimal.valueOf(40.0), java.time.LocalDateTime.now().minusDays(2));
            Transaction buy2 = new Transaction(user, player, TransactionType.BUY, 5,
                    BigDecimal.valueOf(60.0), java.time.LocalDateTime.now().minusDays(1));
            when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
            when(tokenHoldingRepository.findByOwner(user)).thenReturn(List.of(holding));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(70.0));
            when(transactionRepository.findByUserAndPlayerOrderByCreatedAtAsc(user, player))
                    .thenReturn(List.of(buy1, buy2));

            List<PortfolioEntryResponse> portfolio = orderService.getPortfolio("user-id");

            assertThat(portfolio.get(0).getAvgBuyPrice()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        }

        @Test
        @DisplayName("calcula ganancia y porcentaje de ganancia correctamente")
        void computesCorrectProfitLoss() {
            TokenHolding holding = new TokenHolding(player, user, 10);
            Transaction buyTx = new Transaction(user, player, TransactionType.BUY, 10,
                    BigDecimal.valueOf(50.0), java.time.LocalDateTime.now().minusDays(1));
            when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
            when(tokenHoldingRepository.findByOwner(user)).thenReturn(List.of(holding));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(70.0));
            when(transactionRepository.findByUserAndPlayerOrderByCreatedAtAsc(user, player))
                    .thenReturn(List.of(buyTx));

            List<PortfolioEntryResponse> portfolio = orderService.getPortfolio("user-id");
            PortfolioEntryResponse entry = portfolio.get(0);

            assertThat(entry.getGain()).isEqualByComparingTo(BigDecimal.valueOf(200.0));
            assertThat(entry.getGainPercent()).isEqualByComparingTo(BigDecimal.valueOf(40.0));
        }

        @Test
        @DisplayName("resetea el precio promedio si el usuario vendió todos los tokens y volvió a comprar")
        void resetsAvgPriceAfterSellingAll() {
            TokenHolding holding = new TokenHolding(player, user, 5);
            Transaction buy1 = new Transaction(user, player, TransactionType.BUY, 5,
                    BigDecimal.valueOf(40.0), java.time.LocalDateTime.now().minusDays(3));
            Transaction sell = new Transaction(user, player, TransactionType.SELL, 5,
                    BigDecimal.valueOf(60.0), java.time.LocalDateTime.now().minusDays(2));
            Transaction buy2 = new Transaction(user, player, TransactionType.BUY, 5,
                    BigDecimal.valueOf(80.0), java.time.LocalDateTime.now().minusDays(1));
            when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
            when(tokenHoldingRepository.findByOwner(user)).thenReturn(List.of(holding));
            when(quoteService.getLatestPriceForPlayer(PLAYER_ID)).thenReturn(BigDecimal.valueOf(90.0));
            when(transactionRepository.findByUserAndPlayerOrderByCreatedAtAsc(user, player))
                    .thenReturn(List.of(buy1, sell, buy2));

            List<PortfolioEntryResponse> portfolio = orderService.getPortfolio("user-id");

            assertThat(portfolio.get(0).getAvgBuyPrice()).isEqualByComparingTo(BigDecimal.valueOf(80.0));
        }

        @Test
        @DisplayName("lanza excepción cuando el usuario no existe")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findById("unknown-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getPortfolio("unknown-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactions {

        @Test
        @DisplayName("devuelve el historial de transacciones del usuario")
        void returnsUserTransactionHistory() {
            Transaction tx = new Transaction(user, player, TransactionType.BUY, 5,
                    BigDecimal.valueOf(50.0), java.time.LocalDateTime.now());
            when(userRepository.findById("user-id")).thenReturn(Optional.of(user));
            when(transactionRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(tx));

            List<TransactionResponse> transactions = orderService.getTransactions("user-id");

            assertThat(transactions).hasSize(1);
        }

        @Test
        @DisplayName("lanza excepción cuando el usuario no existe")
        void throwsExceptionWhenUserNotFound() {
            when(userRepository.findById("unknown-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getTransactions("unknown-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    private static OrderRequest requestOf(String playerId, int quantity) {
        OrderRequest req = new OrderRequest();
        req.setPlayerId(playerId);
        req.setQuantity(quantity);
        return req;
    }
}
