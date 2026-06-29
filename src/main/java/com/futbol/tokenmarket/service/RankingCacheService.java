package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.QuoteStrategy;
import com.futbol.tokenmarket.repository.PlayerQuoteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankingCacheService {

    private final PlayerQuoteRepository quoteRepository;

    public RankingCacheService(PlayerQuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @Cacheable(value = "ranking", key = "#strategy.name()")
    @Transactional(readOnly = true)
    public List<PlayerQuote> getSortedRanking(QuoteStrategy strategy) {
        Map<String, PlayerQuote> latestByPlayerId = new HashMap<>();
        for (PlayerQuote quote : quoteRepository.findByStrategyOrderByCalculatedAtDesc(strategy)) {
            if (quote.getPlayer() == null || quote.getPlayer().getId() == null) {
                continue;
            }
            latestByPlayerId.putIfAbsent(quote.getPlayer().getId(), quote);
        }
        return new ArrayList<>(latestByPlayerId.values()).stream()
                .sorted(Comparator.comparing(PlayerQuote::getValue, Comparator.reverseOrder())
                        .thenComparing(PlayerQuote::getCalculatedAt, Comparator.reverseOrder())
                        .thenComparing(q -> q.getPlayer() != null ? q.getPlayer().getId() : ""))
                .toList();
    }

    @CacheEvict(value = "ranking", allEntries = true)
    public void evictAll() {}
}
