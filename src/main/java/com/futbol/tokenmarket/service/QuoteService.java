package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.QuoteSettings;
import com.futbol.tokenmarket.model.QuoteStrategy;
import com.futbol.tokenmarket.repository.PlayerMatchStatsRepository;
import com.futbol.tokenmarket.repository.PlayerQuoteRepository;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.QuoteSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuoteService {

    private static final double BASE_VALUE = 100.0;
    private static final double SCALE_FACTOR = 18.0;

    private final PlayerRepository playerRepository;
    private final PlayerMatchStatsRepository matchStatsRepository;
    private final PlayerQuoteRepository quoteRepository;
    private final QuoteSettingsRepository settingsRepository;

    public QuoteService(PlayerRepository playerRepository,
                        PlayerMatchStatsRepository matchStatsRepository,
                        PlayerQuoteRepository quoteRepository,
                        QuoteSettingsRepository settingsRepository) {
        this.playerRepository = playerRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.quoteRepository = quoteRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional
    public Page<PlayerQuote> recalculateQuotes(String strategyValue, int page, int size) {
        QuoteStrategy strategy = QuoteStrategy.fromApiValue(strategyValue);
        List<Player> players = playerRepository.findAll();
        Map<String, List<PlayerMatchStats>> statsByPlayer = matchStatsRepository.findAll().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(PlayerMatchStats::getPlayerId));
        Map<String, PlayerQuote> latestQuotesByPlayer = latestQuotesForStrategy(strategy).stream()
            .filter(quote -> quote.getPlayer() != null && quote.getPlayer().getId() != null)
            .collect(Collectors.toMap(quote -> quote.getPlayer().getId(), Function.identity(), (left, right) -> left, HashMap::new));

        LocalDateTime recalculatedAt = LocalDateTime.now();
        List<PlayerQuote> quotes = new ArrayList<>(players.size());
        List<PlayerQuote> quotesToPersist = new ArrayList<>();
        for (Player player : players) {
            PlayerQuote generatedQuote = buildQuote(player, statsByPlayer.getOrDefault(player.getId(), List.of()), strategy, recalculatedAt);
            PlayerQuote latestQuote = latestQuotesByPlayer.get(player.getId());
            if (isSameQuote(latestQuote, generatedQuote)) {
                quotes.add(latestQuote);
                continue;
            }
            quotes.add(generatedQuote);
            quotesToPersist.add(generatedQuote);
        }

        if (!quotesToPersist.isEmpty()) {
            quoteRepository.saveAll(quotesToPersist);
        }
        persistActiveStrategy(strategy, recalculatedAt);

        List<PlayerQuote> ordered = quotes.stream()
            .sorted(Comparator.comparing(PlayerQuote::getValue, Comparator.reverseOrder())
                .thenComparing(PlayerQuote::getCalculatedAt, Comparator.reverseOrder())
                .thenComparing(quote -> quote.getPlayer() != null ? quote.getPlayer().getId() : ""))
            .toList();
        return slice(ordered, page, size);
    }

    public Page<PlayerQuote> getPlayerQuoteHistory(String playerId, int page, int size) {
        return slice(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(playerId), page, size);
    }

    public Page<PlayerQuote> getRanking(int page, int size) {
        QuoteStrategy strategy = getActiveStrategy();
        List<PlayerQuote> latestByPlayer = latestQuotesForStrategy(strategy);
        List<PlayerQuote> ordered = latestByPlayer.stream()
            .sorted(Comparator.comparing(PlayerQuote::getValue, Comparator.reverseOrder())
                .thenComparing(PlayerQuote::getCalculatedAt, Comparator.reverseOrder())
                .thenComparing(quote -> quote.getPlayer() != null ? quote.getPlayer().getId() : ""))
            .toList();
        return slice(ordered, page, size);
    }

    public QuoteStrategy getActiveStrategy() {
        return settingsRepository.findActiveSettings()
            .map(settings -> QuoteStrategy.fromApiValue(settings.getActiveStrategy()))
            .orElse(QuoteStrategy.BALANCED);
    }

    private List<PlayerQuote> latestQuotesForStrategy(QuoteStrategy strategy) {
        Map<String, PlayerQuote> latestByPlayerId = new HashMap<>();
        for (PlayerQuote quote : quoteRepository.findByStrategyOrderByCalculatedAtDesc(strategy)) {
            if (quote.getPlayer() == null || quote.getPlayer().getId() == null) {
                continue;
            }
            latestByPlayerId.putIfAbsent(quote.getPlayer().getId(), quote);
        }
        return new ArrayList<>(latestByPlayerId.values());
    }

    private void persistActiveStrategy(QuoteStrategy strategy, LocalDateTime updatedAt) {
        QuoteSettings settings = settingsRepository.findActiveSettings().orElseGet(QuoteSettings::new);
        settings.setActiveStrategy(strategy.getApiValue());
        settings.setUpdatedAt(updatedAt);
        settingsRepository.save(settings);
    }

    private PlayerQuote buildQuote(Player player, List<PlayerMatchStats> stats, QuoteStrategy strategy, LocalDateTime recalculatedAt) {
        ScoreBreakdown breakdown = strategy == QuoteStrategy.POSITION_AWARE
            ? positionAwareBreakdown(player, stats)
            : balancedBreakdown(stats);

        PlayerQuote quote = new PlayerQuote();
        quote.setPlayer(player);
        quote.setStrategy(strategy);
        quote.setScore(round(breakdown.score(), 4));
        quote.setValue(round(Math.max(1.0, BASE_VALUE + breakdown.score() * SCALE_FACTOR), 2));
        quote.setCalculatedAt(recalculatedAt);
        quote.setBreakdown(breakdown.description());
        return quote;
    }

    private ScoreBreakdown balancedBreakdown(List<PlayerMatchStats> stats) {
        double rating = normalizeRating(average(stats, PlayerMatchStats::getRating));
        double goals = per90Average(stats, PlayerMatchStats::getGoals);
        double assists = per90Average(stats, PlayerMatchStats::getAssists);
        double shots = per90Average(stats, PlayerMatchStats::getShots);
        double keyPasses = per90Average(stats, PlayerMatchStats::getKeyPasses);
        double dribbles = per90Average(stats, PlayerMatchStats::getDribblesWon);
        double tackles = per90Average(stats, PlayerMatchStats::getTackles);
        double interceptions = per90Average(stats, PlayerMatchStats::getInterceptions);
        double blockedShots = per90Average(stats, PlayerMatchStats::getBlockedShots);
        double passSuccess = normalizePercent(average(stats, PlayerMatchStats::getPassSuccess));
        double disciplinePenalty = per90Average(stats, PlayerMatchStats::getYellowCards) * 0.06
            + per90Average(stats, PlayerMatchStats::getRedCards) * 0.25;

        double score = (rating * 0.30)
            + (goals * 0.18)
            + (assists * 0.12)
            + (shots * 0.10)
            + (keyPasses * 0.10)
            + (dribbles * 0.08)
            + (tackles * 0.06)
            + (interceptions * 0.05)
            + (blockedShots * 0.04)
            + (passSuccess * 0.05)
            - disciplinePenalty;

        return new ScoreBreakdown(score, buildBreakdown(
            "balanced",
            stats.size(),
            Map.ofEntries(
                Map.entry("rating", contribution(rating, 0.30)),
                Map.entry("goals", contribution(goals, 0.18)),
                Map.entry("assists", contribution(assists, 0.12)),
                Map.entry("shots", contribution(shots, 0.10)),
                Map.entry("keyPasses", contribution(keyPasses, 0.10)),
                Map.entry("dribbles", contribution(dribbles, 0.08)),
                Map.entry("tackles", contribution(tackles, 0.06)),
                Map.entry("interceptions", contribution(interceptions, 0.05)),
                Map.entry("blockedShots", contribution(blockedShots, 0.04)),
                Map.entry("passSuccess", contribution(passSuccess, 0.05)),
                Map.entry("disciplinePenalty", -disciplinePenalty)
            )));
    }

    private ScoreBreakdown positionAwareBreakdown(Player player, List<PlayerMatchStats> stats) {
        String position = player != null ? player.getPosition() : null;
        double rating = normalizeRating(average(stats, PlayerMatchStats::getRating));
        double goals = per90Average(stats, PlayerMatchStats::getGoals);
        double assists = per90Average(stats, PlayerMatchStats::getAssists);
        double shots = per90Average(stats, PlayerMatchStats::getShots);
        double keyPasses = per90Average(stats, PlayerMatchStats::getKeyPasses);
        double dribbles = per90Average(stats, PlayerMatchStats::getDribblesWon);
        double tackles = per90Average(stats, PlayerMatchStats::getTackles);
        double interceptions = per90Average(stats, PlayerMatchStats::getInterceptions);
        double clearances = per90Average(stats, PlayerMatchStats::getClearances);
        double blockedShots = per90Average(stats, PlayerMatchStats::getBlockedShots);
        double aerialsWon = per90Average(stats, PlayerMatchStats::getAerialsWon);
        double passSuccess = normalizePercent(average(stats, PlayerMatchStats::getPassSuccess));
        double disciplinePenalty = per90Average(stats, PlayerMatchStats::getYellowCards) * 0.06
            + per90Average(stats, PlayerMatchStats::getRedCards) * 0.25;

        String normalizedPosition = position == null ? "" : position.toUpperCase(Locale.ROOT);
        if (normalizedPosition.contains("GK")) {
            double score = (rating * 0.42)
                + (blockedShots * 0.18)
                + (aerialsWon * 0.10)
                + (clearances * 0.12)
                + (passSuccess * 0.08)
                + (tackles * 0.05)
                + (interceptions * 0.05)
                - disciplinePenalty;
            return new ScoreBreakdown(score, buildBreakdown(
                "position-aware/GK",
                stats.size(),
                Map.ofEntries(
                    Map.entry("rating", contribution(rating, 0.42)),
                    Map.entry("blockedShots", contribution(blockedShots, 0.18)),
                    Map.entry("aerialsWon", contribution(aerialsWon, 0.10)),
                    Map.entry("clearances", contribution(clearances, 0.12)),
                    Map.entry("passSuccess", contribution(passSuccess, 0.08)),
                    Map.entry("tackles", contribution(tackles, 0.05)),
                    Map.entry("interceptions", contribution(interceptions, 0.05)),
                    Map.entry("disciplinePenalty", -disciplinePenalty)
                )));
        }
        if (normalizedPosition.contains("D") || normalizedPosition.contains("DM")) {
            double score = (rating * 0.28)
                + (tackles * 0.20)
                + (interceptions * 0.16)
                + (clearances * 0.14)
                + (blockedShots * 0.10)
                + (aerialsWon * 0.06)
                + (passSuccess * 0.06)
                + (goals * 0.04)
                - disciplinePenalty;
            return new ScoreBreakdown(score, buildBreakdown(
                "position-aware/defense",
                stats.size(),
                Map.ofEntries(
                    Map.entry("rating", contribution(rating, 0.28)),
                    Map.entry("tackles", contribution(tackles, 0.20)),
                    Map.entry("interceptions", contribution(interceptions, 0.16)),
                    Map.entry("clearances", contribution(clearances, 0.14)),
                    Map.entry("blockedShots", contribution(blockedShots, 0.10)),
                    Map.entry("aerialsWon", contribution(aerialsWon, 0.06)),
                    Map.entry("passSuccess", contribution(passSuccess, 0.06)),
                    Map.entry("goals", contribution(goals, 0.04)),
                    Map.entry("disciplinePenalty", -disciplinePenalty)
                )));
        }
        if (normalizedPosition.contains("M") || normalizedPosition.contains("AM")) {
            double score = (rating * 0.25)
                + (keyPasses * 0.18)
                + (assists * 0.14)
                + (goals * 0.10)
                + (shots * 0.09)
                + (dribbles * 0.08)
                + (tackles * 0.07)
                + (interceptions * 0.05)
                + (passSuccess * 0.09)
                - disciplinePenalty;
            return new ScoreBreakdown(score, buildBreakdown(
                "position-aware/midfield",
                stats.size(),
                Map.ofEntries(
                    Map.entry("rating", contribution(rating, 0.25)),
                    Map.entry("keyPasses", contribution(keyPasses, 0.18)),
                    Map.entry("assists", contribution(assists, 0.14)),
                    Map.entry("goals", contribution(goals, 0.10)),
                    Map.entry("shots", contribution(shots, 0.09)),
                    Map.entry("dribbles", contribution(dribbles, 0.08)),
                    Map.entry("tackles", contribution(tackles, 0.07)),
                    Map.entry("interceptions", contribution(interceptions, 0.05)),
                    Map.entry("passSuccess", contribution(passSuccess, 0.09)),
                    Map.entry("disciplinePenalty", -disciplinePenalty)
                )));
        }
        if (normalizedPosition.contains("FW")) {
            double score = (rating * 0.22)
                + (goals * 0.28)
                + (assists * 0.14)
                + (shots * 0.16)
                + (keyPasses * 0.08)
                + (dribbles * 0.08)
                + (passSuccess * 0.04)
                - disciplinePenalty;
            return new ScoreBreakdown(score, buildBreakdown(
                "position-aware/forward",
                stats.size(),
                Map.ofEntries(
                    Map.entry("rating", contribution(rating, 0.22)),
                    Map.entry("goals", contribution(goals, 0.28)),
                    Map.entry("assists", contribution(assists, 0.14)),
                    Map.entry("shots", contribution(shots, 0.16)),
                    Map.entry("keyPasses", contribution(keyPasses, 0.08)),
                    Map.entry("dribbles", contribution(dribbles, 0.08)),
                    Map.entry("passSuccess", contribution(passSuccess, 0.04)),
                    Map.entry("disciplinePenalty", -disciplinePenalty)
                )));
        }

        return balancedBreakdown(stats);
    }

    private double contribution(double metricValue, double weight) {
        return metricValue * weight;
    }

    private String buildBreakdown(String strategy, int samples, Map<String, Double> parts) {
        StringBuilder builder = new StringBuilder();
        builder.append("strategy=").append(strategy)
            .append(",samples=").append(samples)
            .append(",components=");

        boolean first = true;
        for (Map.Entry<String, Double> entry : parts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList()) {
            if (!first) {
                builder.append(" + ");
            }
            first = false;
            builder.append(entry.getKey()).append("(").append(round(entry.getValue(), 4)).append(")");
        }
        return builder.toString();
    }

    private boolean isSameQuote(PlayerQuote existingQuote, PlayerQuote generatedQuote) {
        if (existingQuote == null || generatedQuote == null) {
            return false;
        }
        return existingQuote.getStrategy() == generatedQuote.getStrategy()
            && Double.compare(existingQuote.getScore(), generatedQuote.getScore()) == 0
            && Double.compare(existingQuote.getValue(), generatedQuote.getValue()) == 0
            && Objects.equals(existingQuote.getBreakdown(), generatedQuote.getBreakdown());
    }

    private record ScoreBreakdown(double score, String description) {
    }

    private double average(List<PlayerMatchStats> stats, Function<PlayerMatchStats, ? extends Number> extractor) {
        if (stats == null || stats.isEmpty()) {
            return 0.0;
        }
        return stats.stream()
            .map(extractor)
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .average()
            .orElse(0.0);
    }

    private double per90Average(List<PlayerMatchStats> stats, Function<PlayerMatchStats, ? extends Number> extractor) {
        if (stats == null || stats.isEmpty()) {
            return 0.0;
        }
        // Use the real/statistical value per match (raw average) instead of normalizing to per-90.
        // This prevents very short appearances from inflating metrics (e.g. 1 tackle in 5 minutes -> 18/90).
        return stats.stream()
            .map(extractor)
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .average()
            .orElse(0.0);
    }

    private double per90(PlayerMatchStats stat, Function<PlayerMatchStats, ? extends Number> extractor) {
        if (stat == null) {
            return 0.0;
        }
        double minutes = stat.getMinutesPlayed() == null || stat.getMinutesPlayed() <= 0 ? 90.0 : stat.getMinutesPlayed();
        Number rawValue = extractor.apply(stat);
        double value = rawValue == null ? 0.0 : rawValue.doubleValue();
        return Math.max(0.0, value) * 90.0 / minutes;
    }

    private double normalizeRating(double value) {
        return Math.max(0.0, Math.min(10.0, value)) / 2.0;
    }

    private double normalizePercent(double value) {
        return Math.max(0.0, Math.min(100.0, value)) / 20.0;
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private Page<PlayerQuote> slice(List<PlayerQuote> source, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, source.size());
        int toIndex = Math.min(fromIndex + safeSize, source.size());
        return new PageImpl<>(source.subList(fromIndex, toIndex), PageRequest.of(safePage, safeSize), source.size());
    }
}