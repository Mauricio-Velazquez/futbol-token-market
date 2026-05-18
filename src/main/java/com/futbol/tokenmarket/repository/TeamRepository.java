package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class TeamRepository {

    private static final Logger log = LoggerFactory.getLogger(TeamRepository.class);

    @PersistenceContext
    private EntityManager em;

    public List<Team> findAll() {
        return em.createQuery("SELECT t FROM Team t", Team.class).getResultList();
    }

    public List<Team> findByLeague(String league) {
        return em.createQuery(
                "SELECT t FROM Team t WHERE LOWER(t.league) = LOWER(:league)", Team.class)
            .setParameter("league", league)
            .getResultList();
    }

    @Transactional
    public List<Team> saveTeamsForLeague(String league, List<Team> teams) {
        em.createQuery("DELETE FROM Team t WHERE LOWER(t.league) = LOWER(:league)")
            .setParameter("league", league)
            .executeUpdate();
        em.flush();
        em.clear();
        teams.forEach(em::persist);
        log.info("[TeamRepository] Guardado {} equipos para liga: {}", teams.size(), league);
        return teams;
    }
}
