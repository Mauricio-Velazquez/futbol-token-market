package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.QuoteStrategy;
import com.futbol.tokenmarket.repository.PlayerRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MarketMetricsService {

    private final PlayerRepository playerRepository;
    private final QuoteService quoteService;
    private final RankingCacheService rankingCacheService;
    private final MeterRegistry registry;

    public MarketMetricsService(PlayerRepository playerRepository,
                                QuoteService quoteService,
                                RankingCacheService rankingCacheService,
                                MeterRegistry registry) {
        this.playerRepository = playerRepository;
        this.quoteService = quoteService;
        this.rankingCacheService = rankingCacheService;
        this.registry = registry;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("market.players.total", this, m -> safeCount(() ->
                        playerRepository.findAll().size()))
                .description("Total de jugadores registrados en el mercado")
                .register(registry);

        Gauge.builder("market.token.price.avg", this, m -> safeCount(() ->
                        activeRanking().stream().mapToDouble(PlayerQuote::getValue).average().orElse(0.0)))
                .description("Precio promedio de tokens según estrategia activa")
                .baseUnit("tokens")
                .register(registry);

        Gauge.builder("market.token.price.max", this, m -> safeCount(() ->
                        activeRanking().stream().mapToDouble(PlayerQuote::getValue).max().orElse(0.0)))
                .description("Precio máximo de token según estrategia activa")
                .baseUnit("tokens")
                .register(registry);

        Gauge.builder("market.token.price.min", this, m -> safeCount(() ->
                        activeRanking().stream().mapToDouble(PlayerQuote::getValue).min().orElse(0.0)))
                .description("Precio mínimo de token según estrategia activa")
                .baseUnit("tokens")
                .register(registry);
    }

    private List<PlayerQuote> activeRanking() {
        QuoteStrategy strategy = quoteService.getActiveStrategy();
        return rankingCacheService.getSortedRanking(strategy);
    }

    private double safeCount(java.util.concurrent.Callable<Number> supplier) {
        try {
            return supplier.call().doubleValue();
        } catch (Exception e) {
            return -1.0;
        }
    }
}
