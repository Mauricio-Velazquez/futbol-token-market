package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.TokenHolding;
import com.futbol.tokenmarket.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class TokenHoldingRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<TokenHolding> findByPlayerAndOwner(Player player, User owner) {
        List<TokenHolding> results = em.createQuery(
                "SELECT t FROM TokenHolding t WHERE t.player = :player AND t.owner = :owner",
                TokenHolding.class)
            .setParameter("player", player)
            .setParameter("owner", owner)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<TokenHolding> findByOwner(User owner) {
        return em.createQuery(
                "SELECT t FROM TokenHolding t WHERE t.owner = :owner", TokenHolding.class)
            .setParameter("owner", owner)
            .getResultList();
    }

    public Set<String> findPlayerIdsWithHoldings(List<String> playerIds) {
        if (playerIds.isEmpty()) {
            return new HashSet<>();
        }
        List<String> results = em.createQuery(
                "SELECT DISTINCT t.player.id FROM TokenHolding t WHERE t.player.id IN :playerIds",
                String.class)
            .setParameter("playerIds", playerIds)
            .getResultList();
        return new HashSet<>(results);
    }

    @Transactional
    public TokenHolding save(TokenHolding holding) {
        return em.merge(holding);
    }

    @Transactional
    public void saveAll(List<TokenHolding> holdings) {
        holdings.forEach(em::merge);
    }
}
