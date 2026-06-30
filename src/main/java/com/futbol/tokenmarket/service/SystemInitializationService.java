package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.TokenHolding;
import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.model.Wallet;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TokenHoldingRepository;
import com.futbol.tokenmarket.repository.UserRepository;
import com.futbol.tokenmarket.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SystemInitializationService implements ApplicationRunner {

    public static final String SISTEMA_USERNAME = "sistema";

    private static final Logger log = LoggerFactory.getLogger(SystemInitializationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletRepository walletRepository;
    private final TokenHoldingRepository tokenHoldingRepository;
    private final PlayerRepository playerRepository;

    public SystemInitializationService(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       WalletRepository walletRepository,
                                       TokenHoldingRepository tokenHoldingRepository,
                                       PlayerRepository playerRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletRepository = walletRepository;
        this.tokenHoldingRepository = tokenHoldingRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        User sistema = ensureSistemaUser();
        ensureWallet(sistema);
        issueTokensForExistingPlayers(sistema);
    }

    private User ensureSistemaUser() {
        return userRepository.findByUsername(SISTEMA_USERNAME).orElseGet(() -> {
            User sistema = new User(UUID.randomUUID().toString(), SISTEMA_USERNAME,
                    passwordEncoder.encode(UUID.randomUUID().toString()));
            User saved = userRepository.save(sistema);
            log.info("[SystemInit] Usuario '{}' creado", SISTEMA_USERNAME);
            return saved;
        });
    }

    private void ensureWallet(User sistema) {
        if (walletRepository.findByUser(sistema).isEmpty()) {
            walletRepository.save(new Wallet(sistema));
            log.info("[SystemInit] Billetera creada para '{}'", SISTEMA_USERNAME);
        }
    }

    private void issueTokensForExistingPlayers(User sistema) {
        List<Player> players = playerRepository.findAll();
        if (players.isEmpty()) {
            return;
        }

        List<String> playerIds = players.stream().map(Player::getId).toList();
        Set<String> alreadyIssued = tokenHoldingRepository.findPlayerIdsWithHoldings(playerIds);

        List<TokenHolding> toCreate = players.stream()
                .filter(p -> !alreadyIssued.contains(p.getId()))
                .map(p -> new TokenHolding(p, sistema, TokenHolding.TOKENS_PER_PLAYER))
                .collect(Collectors.toList());

        if (!toCreate.isEmpty()) {
            tokenHoldingRepository.saveAll(toCreate);
            log.info("[SystemInit] {} tokens emitidos para jugadores sin holdings", toCreate.size());
        }
    }
}
