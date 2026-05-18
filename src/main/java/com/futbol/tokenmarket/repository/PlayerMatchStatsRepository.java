package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.PlayerMatchStats;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class PlayerMatchStatsRepository {

    private static final Logger log = LoggerFactory.getLogger(PlayerMatchStatsRepository.class);

    @PersistenceContext
    private EntityManager em;

    public List<PlayerMatchStats> findAll() {
        return em.createQuery("SELECT s FROM PlayerMatchStats s", PlayerMatchStats.class)
            .getResultList();
    }

    public Set<String> getExistingMatchIds() {
        List<String> ids = em.createQuery(
                "SELECT DISTINCT s.matchId FROM PlayerMatchStats s", String.class)
            .getResultList();
        return new HashSet<>(ids);
    }

    @Transactional
    public void saveAllIfNew(List<PlayerMatchStats> stats) {
        stats.forEach(em::merge);
        log.info("[PlayerMatchStats] Guardados {} registros", stats.size());
    }
}
