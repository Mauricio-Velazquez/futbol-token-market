package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.Team;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class PlayerRepository {

    private static final Map<String, Pattern> POSITION_PATTERNS = Map.of(
        "GK", Pattern.compile("(?:^|,)GK(?:,|$)"),
        "D",  Pattern.compile("(?:^|,)D\\("),
        "DM", Pattern.compile("(?:^|,)DM[CLR](?:,|$)"),
        "M",  Pattern.compile("(?:^|,)M\\("),
        "AM", Pattern.compile("(?:^|,)AM\\("),
        "FW", Pattern.compile("(?:^|,)FW(?:,|$)")
    );

    @PersistenceContext
    private EntityManager em;

    public List<Player> findAll() {
        return em.createQuery("SELECT p FROM Player p", Player.class).getResultList();
    }

    public Optional<Player> findById(String id) {
        return Optional.ofNullable(em.find(Player.class, id));
    }

    public List<Player> findByLeague(String league) {
        return em.createQuery(
                "SELECT p FROM Player p WHERE LOWER(p.league) = LOWER(:league)", Player.class)
            .setParameter("league", league)
            .getResultList();
    }

    public List<Player> findByFilters(String league, String team, String position) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM Player p WHERE 1=1");
        if (league != null && !league.isBlank()) jpql.append(" AND LOWER(p.league) = LOWER(:league)");
        if (team != null && !team.isBlank()) jpql.append(" AND LOWER(p.team.name) = LOWER(:team)");

        TypedQuery<Player> query = em.createQuery(jpql.toString(), Player.class);
        if (league != null && !league.isBlank()) query.setParameter("league", league);
        if (team != null && !team.isBlank()) query.setParameter("team", team);

        List<Player> players = query.getResultList();

        if (position != null && !position.isBlank()) {
            Pattern pattern = POSITION_PATTERNS.get(position.toUpperCase());
            if (pattern != null) {
                players = players.stream()
                    .filter(p -> pattern.matcher(p.getPosition()).find())
                    .toList();
            }
        }
        return players;
    }

    @Transactional
    public Player save(Player player) {
        normalizeTeamName(player);
        return em.merge(player);
    }

    @Transactional
    public boolean saveAll(List<Player> players) {
        players.forEach(this::normalizeTeamName);
        players.forEach(em::merge);
        return true;
    }

    @Transactional
    public synchronized boolean savePlayersForLeague(String league, List<Player> players) {
        em.createQuery("DELETE FROM Player p WHERE LOWER(p.league) = LOWER(:league)")
            .setParameter("league", league)
            .executeUpdate();
        em.flush();
        em.clear();
        players.forEach(this::normalizeTeamName);
        players.forEach(em::merge);
        return true;
    }

    private void normalizeTeamName(Player player) {
        Team team = player.getTeam();
        if (player.getTeamName() == null && team != null) {
            player.setTeamName(team.getName());
        }
    }

    @Transactional
    public boolean deleteById(String id) {
        Player player = em.find(Player.class, id);
        if (player == null) return false;
        em.remove(player);
        return true;
    }
}
