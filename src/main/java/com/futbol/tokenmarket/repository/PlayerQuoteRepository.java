package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.QuoteStrategy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class PlayerQuoteRepository {

    @PersistenceContext
    private EntityManager em;

    public List<PlayerQuote> findByPlayerIdOrderByCalculatedAtDesc(String playerId) {
        return em.createQuery(
                "SELECT q FROM PlayerQuote q WHERE q.player.id = :playerId ORDER BY q.calculatedAt DESC, q.id DESC",
                PlayerQuote.class)
            .setParameter("playerId", playerId)
            .getResultList();
    }

    public List<PlayerQuote> findByStrategyOrderByCalculatedAtDesc(QuoteStrategy strategy) {
        return em.createQuery(
                "SELECT q FROM PlayerQuote q WHERE q.strategy = :strategy ORDER BY q.calculatedAt DESC, q.id DESC",
                PlayerQuote.class)
            .setParameter("strategy", strategy)
            .getResultList();
    }

    public List<PlayerQuote> findByPlayerIdAndStrategyOrderByCalculatedAtDesc(String playerId, QuoteStrategy strategy) {
        return em.createQuery(
                "SELECT q FROM PlayerQuote q WHERE q.player.id = :playerId AND q.strategy = :strategy ORDER BY q.calculatedAt DESC, q.id DESC",
                PlayerQuote.class)
            .setParameter("playerId", playerId)
            .setParameter("strategy", strategy)
            .getResultList();
    }

    @Transactional
    public void saveAll(List<PlayerQuote> quotes) {
        quotes.forEach(em::persist);
    }
}