package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.dto.OrderRequest;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.model.Wallet;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.UserRepository;
import com.futbol.tokenmarket.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);
    private static final String SEED_PASSWORD = "seed_password_2025";

    private enum Profile { ACUMULADOR, COLECCIONISTA, DIVERSIFICADO, ESTRELLA, MINI, RANDOM }
    private record UserSeed(String username, Profile profile, int budget) {}

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlayerRepository playerRepository;
    private final OrderService orderService;
    private final QuoteService quoteService;

    public DataSeederService(UserRepository userRepository,
                             WalletRepository walletRepository,
                             PasswordEncoder passwordEncoder,
                             PlayerRepository playerRepository,
                             OrderService orderService,
                             QuoteService quoteService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.playerRepository = playerRepository;
        this.orderService = orderService;
        this.quoteService = quoteService;
    }

    public int seed() {
        List<Player> allPlayers = playerRepository.findAll();
        if (allPlayers.isEmpty()) {
            log.warn("[Seeder] No hay jugadores cargados. Ejecutar scraping primero.");
            return 0;
        }

        int created = 0;
        for (UserSeed seed : buildSeeds()) {
            if (userRepository.findByUsername(seed.username()).isPresent()) {
                log.info("[Seeder] '{}' ya existe, saltando", seed.username());
                continue;
            }
            createUser(seed.username(), seed.budget());
            buyTokens(seed.username(), allPlayers, seed.budget(), seed.profile());
            created++;
        }
        log.info("[Seeder] Completado: {} usuarios creados", created);
        return created;
    }

    private void createUser(String username, int budget) {
        User user = new User(UUID.randomUUID().toString(), username, passwordEncoder.encode(SEED_PASSWORD));
        userRepository.save(user);
        Wallet wallet = new Wallet(user);
        wallet.setBalance(BigDecimal.valueOf(budget));
        walletRepository.save(wallet);
    }

    private void buyTokens(String username, List<Player> allPlayers, int budget, Profile profile) {
        BigDecimal initialBudget = BigDecimal.valueOf(budget);
        BigDecimal target = initialBudget.multiply(BigDecimal.valueOf(0.80));
        BigDecimal spent = BigDecimal.ZERO;
        Random rng = new Random(username.hashCode());

        List<Player> candidates = buildCandidateList(allPlayers, profile, rng);

        for (Player player : candidates) {
            if (spent.compareTo(target) >= 0) break;
            BigDecimal remaining = initialBudget.subtract(spent);
            BigDecimal price = quoteService.getLatestPriceForPlayer(player.getId());
            if (remaining.compareTo(price) < 0) continue;

            int maxAffordable = remaining.divide(price, 0, RoundingMode.FLOOR).intValue();
            int qty = resolveQty(profile, maxAffordable, rng);
            if (qty <= 0) continue;

            OrderRequest req = new OrderRequest();
            req.setPlayerId(player.getId());
            req.setQuantity(qty);
            try {
                orderService.buy(username, req);
                spent = spent.add(price.multiply(BigDecimal.valueOf(qty)));
            } catch (Exception e) {
                log.debug("[Seeder] Skip {} × '{}': {}", qty, player.getName(), e.getMessage());
            }
        }

        BigDecimal pct = spent.divide(initialBudget, 4, RoundingMode.HALF_UP)
                              .multiply(BigDecimal.valueOf(100))
                              .setScale(1, RoundingMode.HALF_UP);
        log.info("[Seeder] {} ({}) gastó {} / {} ({}%)", username, profile, spent, budget, pct);
    }

    private List<Player> buildCandidateList(List<Player> allPlayers, Profile profile, Random rng) {
        List<Player> list = new ArrayList<>(allPlayers);
        if (profile == Profile.ESTRELLA) {
            Map<String, BigDecimal> prices = new HashMap<>();
            allPlayers.forEach(p -> prices.put(p.getId(), quoteService.getLatestPriceForPlayer(p.getId())));
            list.sort(Comparator.comparing((Player p) -> prices.get(p.getId())).reversed());
        } else {
            Collections.shuffle(list, rng);
        }
        return list;
    }

    private int resolveQty(Profile profile, int maxAffordable, Random rng) {
        if (maxAffordable <= 0) return 0;
        return switch (profile) {
            case ACUMULADOR    -> maxAffordable;
            case COLECCIONISTA -> Math.min(rng.nextInt(5, 21), maxAffordable);
            case DIVERSIFICADO -> Math.min(rng.nextInt(1, 4),  maxAffordable);
            case ESTRELLA      -> Math.min(rng.nextInt(5, 16), maxAffordable);
            case MINI          -> Math.min(rng.nextInt(1, 3),  maxAffordable);
            case RANDOM        -> rng.nextInt(1, maxAffordable + 1);
        };
    }

    private List<UserSeed> buildSeeds() {
        return List.of(
            // ACUMULADOR (6) — todo en el máximo posible de 1 jugador
            new UserSeed("seed_acum_01", Profile.ACUMULADOR, 2500),
            new UserSeed("seed_acum_02", Profile.ACUMULADOR, 3000),
            new UserSeed("seed_acum_03", Profile.ACUMULADOR, 4000),
            new UserSeed("seed_acum_04", Profile.ACUMULADOR, 2000),
            new UserSeed("seed_acum_05", Profile.ACUMULADOR, 5000),
            new UserSeed("seed_acum_06", Profile.ACUMULADOR, 3500),
            // COLECCIONISTA (7) — 3-6 jugadores elegidos, 5-20 tokens c/u
            new UserSeed("seed_col_01", Profile.COLECCIONISTA, 1500),
            new UserSeed("seed_col_02", Profile.COLECCIONISTA, 2000),
            new UserSeed("seed_col_03", Profile.COLECCIONISTA, 2500),
            new UserSeed("seed_col_04", Profile.COLECCIONISTA, 3000),
            new UserSeed("seed_col_05", Profile.COLECCIONISTA, 3500),
            new UserSeed("seed_col_06", Profile.COLECCIONISTA, 4000),
            new UserSeed("seed_col_07", Profile.COLECCIONISTA, 2200),
            // DIVERSIFICADO (8) — muchos jugadores, 1-3 tokens c/u
            new UserSeed("seed_div_01", Profile.DIVERSIFICADO, 3000),
            new UserSeed("seed_div_02", Profile.DIVERSIFICADO, 3500),
            new UserSeed("seed_div_03", Profile.DIVERSIFICADO, 4000),
            new UserSeed("seed_div_04", Profile.DIVERSIFICADO, 4500),
            new UserSeed("seed_div_05", Profile.DIVERSIFICADO, 5000),
            new UserSeed("seed_div_06", Profile.DIVERSIFICADO, 5500),
            new UserSeed("seed_div_07", Profile.DIVERSIFICADO, 6000),
            new UserSeed("seed_div_08", Profile.DIVERSIFICADO, 3200),
            // ESTRELLA (6) — top jugadores por cotización, 5-15 tokens c/u
            new UserSeed("seed_star_01", Profile.ESTRELLA, 2000),
            new UserSeed("seed_star_02", Profile.ESTRELLA, 2500),
            new UserSeed("seed_star_03", Profile.ESTRELLA, 3000),
            new UserSeed("seed_star_04", Profile.ESTRELLA, 3500),
            new UserSeed("seed_star_05", Profile.ESTRELLA, 4000),
            new UserSeed("seed_star_06", Profile.ESTRELLA, 5000),
            // MINI (5) — bajo presupuesto, 1-2 tokens de varios jugadores
            new UserSeed("seed_mini_01", Profile.MINI, 300),
            new UserSeed("seed_mini_02", Profile.MINI, 450),
            new UserSeed("seed_mini_03", Profile.MINI, 600),
            new UserSeed("seed_mini_04", Profile.MINI, 750),
            new UserSeed("seed_mini_05", Profile.MINI, 800),
            // RANDOM (5) — comportamiento completamente aleatorio
            new UserSeed("seed_rand_01", Profile.RANDOM, 1000),
            new UserSeed("seed_rand_02", Profile.RANDOM, 1800),
            new UserSeed("seed_rand_03", Profile.RANDOM, 2500),
            new UserSeed("seed_rand_04", Profile.RANDOM, 3200),
            new UserSeed("seed_rand_05", Profile.RANDOM, 4000)
        );
    }
}
