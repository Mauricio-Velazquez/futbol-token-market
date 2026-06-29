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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final TokenHoldingRepository tokenHoldingRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final QuoteService quoteService;
    private final Counter buyCounter;
    private final Counter sellCounter;

    public OrderService(UserRepository userRepository,
                        PlayerRepository playerRepository,
                        TokenHoldingRepository tokenHoldingRepository,
                        WalletRepository walletRepository,
                        TransactionRepository transactionRepository,
                        QuoteService quoteService,
                        MeterRegistry registry) {
        this.userRepository = userRepository;
        this.playerRepository = playerRepository;
        this.tokenHoldingRepository = tokenHoldingRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.quoteService = quoteService;
        this.buyCounter = Counter.builder("market.orders.processed")
                .tag("type", "BUY")
                .description("Total de órdenes de compra procesadas")
                .register(registry);
        this.sellCounter = Counter.builder("market.orders.processed")
                .tag("type", "SELL")
                .description("Total de órdenes de venta procesadas")
                .register(registry);
    }

    @Transactional
    public OrderResponse buy(String username, OrderRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        User user = requireUser(username);
        Player player = requirePlayer(request.getPlayerId());
        User sistema = requireSistema();

        BigDecimal price = quoteService.getLatestPriceForPlayer(player.getId());
        BigDecimal totalCost = price.multiply(BigDecimal.valueOf(request.getQuantity()));

        TokenHolding sistemaHolding = tokenHoldingRepository
            .findByPlayerAndOwner(player, sistema)
            .orElseThrow(() -> new IllegalStateException("No hay tokens disponibles para este jugador"));

        if (sistemaHolding.getQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("No hay suficientes tokens disponibles. Disponibles: " + sistemaHolding.getQuantity());
        }

        Wallet wallet = requireWallet(user);
        if (wallet.getBalance().compareTo(totalCost) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente. Requerido: " + totalCost + ", Disponible: " + wallet.getBalance());
        }

        sistemaHolding.setQuantity(sistemaHolding.getQuantity() - request.getQuantity());
        tokenHoldingRepository.save(sistemaHolding);

        TokenHolding userHolding = tokenHoldingRepository
            .findByPlayerAndOwner(player, user)
            .orElse(new TokenHolding(player, user, 0));
        userHolding.setQuantity(userHolding.getQuantity() + request.getQuantity());
        tokenHoldingRepository.save(userHolding);

        wallet.setBalance(wallet.getBalance().subtract(totalCost));
        walletRepository.save(wallet);

        Wallet sistemaWallet = requireWallet(sistema);
        sistemaWallet.setBalance(sistemaWallet.getBalance().add(totalCost));
        walletRepository.save(sistemaWallet);

        transactionRepository.save(new Transaction(user, player, TransactionType.BUY,
                request.getQuantity(), price, LocalDateTime.now()));
        buyCounter.increment();

        return new OrderResponse("BUY", player.getId(), player.getName(),
                request.getQuantity(), price, totalCost, wallet.getBalance());
    }

    @Transactional
    public OrderResponse sell(String username, OrderRequest request) {
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }

        User user = requireUser(username);
        Player player = requirePlayer(request.getPlayerId());
        User sistema = requireSistema();

        BigDecimal price = quoteService.getLatestPriceForPlayer(player.getId());
        BigDecimal totalProceeds = price.multiply(BigDecimal.valueOf(request.getQuantity()));

        TokenHolding userHolding = tokenHoldingRepository
            .findByPlayerAndOwner(player, user)
            .orElseThrow(() -> new IllegalArgumentException("No tenés tokens de este jugador"));

        if (userHolding.getQuantity() < request.getQuantity()) {
            throw new IllegalArgumentException("No tenés suficientes tokens. Disponibles: " + userHolding.getQuantity());
        }

        userHolding.setQuantity(userHolding.getQuantity() - request.getQuantity());
        tokenHoldingRepository.save(userHolding);

        TokenHolding sistemaHolding = tokenHoldingRepository
            .findByPlayerAndOwner(player, sistema)
            .orElse(new TokenHolding(player, sistema, 0));
        sistemaHolding.setQuantity(sistemaHolding.getQuantity() + request.getQuantity());
        tokenHoldingRepository.save(sistemaHolding);

        Wallet wallet = requireWallet(user);
        wallet.setBalance(wallet.getBalance().add(totalProceeds));
        walletRepository.save(wallet);

        Wallet sistemaWallet = requireWallet(sistema);
        sistemaWallet.setBalance(sistemaWallet.getBalance().subtract(totalProceeds));
        walletRepository.save(sistemaWallet);

        transactionRepository.save(new Transaction(user, player, TransactionType.SELL,
                request.getQuantity(), price, LocalDateTime.now()));
        sellCounter.increment();

        return new OrderResponse("SELL", player.getId(), player.getName(),
                request.getQuantity(), price, totalProceeds, wallet.getBalance());
    }

    public List<PortfolioEntryResponse> getPortfolio(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        return tokenHoldingRepository.findByOwner(user).stream()
            .filter(h -> h.getQuantity() > 0)
            .map(h -> new PortfolioEntryResponse(
                h.getPlayer().getId(),
                h.getPlayer().getName(),
                h.getQuantity(),
                quoteService.getLatestPriceForPlayer(h.getPlayer().getId()),
                computeAvgBuyPrice(user, h.getPlayer())))
            .toList();
    }

    private BigDecimal computeAvgBuyPrice(User user, Player player) {
        List<Transaction> txs = transactionRepository
            .findByUserAndPlayerOrderByCreatedAtAsc(user, player);
        BigDecimal avg = BigDecimal.ZERO;
        BigDecimal qty = BigDecimal.ZERO;
        for (Transaction tx : txs) {
            BigDecimal txQty = BigDecimal.valueOf(tx.getQuantity());
            if (tx.getType() == TransactionType.BUY) {
                BigDecimal newQty = qty.add(txQty);
                avg = avg.multiply(qty)
                    .add(tx.getPricePerToken().multiply(txQty))
                    .divide(newQty, 2, RoundingMode.HALF_UP);
                qty = newQty;
            } else {
                qty = qty.subtract(txQty);
                if (qty.compareTo(BigDecimal.ZERO) == 0) {
                    avg = BigDecimal.ZERO;
                }
            }
        }
        return avg;
    }

    public List<TransactionResponse> getTransactions(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        return transactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(TransactionResponse::new)
            .toList();
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    private Player requirePlayer(String playerId) {
        return playerRepository.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado: " + playerId));
    }

    private User requireSistema() {
        return userRepository.findByUsername(SystemInitializationService.SISTEMA_USERNAME)
            .orElseThrow(() -> new IllegalStateException("Usuario sistema no encontrado"));
    }

    private Wallet requireWallet(User user) {
        return walletRepository.findByUser(user)
            .orElseThrow(() -> new IllegalStateException("Billetera no encontrada para usuario: " + user.getUsername()));
    }
}
