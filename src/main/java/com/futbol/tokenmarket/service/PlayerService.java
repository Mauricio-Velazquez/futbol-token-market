package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.repository.PlayerMatchStatsRepository;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);
    private static final String LOG_PREFIX = "[PlayerService] ";

    private final PlayerRepository repository;
    private final TeamRepository teamRepository;
    private final PlayerMatchStatsRepository matchStatsRepository;
    private final WhoScoredScraperService whoScoredScraperService;

    public PlayerService(PlayerRepository repository,
                         TeamRepository teamRepository,
                         PlayerMatchStatsRepository matchStatsRepository,
                         WhoScoredScraperService whoScoredScraperService) {
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.whoScoredScraperService = whoScoredScraperService;
    }

    public List<LeagueStats> getLeagueStats() {
        List<Player> allPlayers = repository.findAll();

        return allPlayers.stream()
                .collect(Collectors.groupingBy(Player::getLeague, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new LeagueStats(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(LeagueStats::getLeague))
                .toList();
    }

    public List<Player> getAllPlayers() {
        return repository.findAll();
    }

    public List<Player> getPlayersByLeague(String league) {
        return repository.findByLeague(league);
    }

    public List<Player> getFilteredPlayers(String league, String team, String position) {
        return repository.findByFilters(league, team, position);
    }

    public Optional<Player> getPlayerById(String id) {
        return repository.findById(id);
    }

    public List<Team> scrapeTeamUrls(String leagueName) {
        Map<String, String> scraped = whoScoredScraperService.scrapeTeamUrls(leagueName);

        List<Team> teams = scraped.entrySet().stream()
                .map(e -> new Team(e.getKey(), e.getValue(), leagueName))
                .toList();

        return teamRepository.saveTeamsForLeague(leagueName, teams);
    }

    public List<PlayerMatchStats> scrapeMatchStatsByMatchday(String leagueName) {
        List<String> matchUrls = whoScoredScraperService.scrapeLeagueMatchUrls(leagueName);
        if (matchUrls.isEmpty()) {
            log.info(LOG_PREFIX + "{}: no se encontraron partidos en la página de la liga", leagueName);
            return List.of();
        }
        log.info(LOG_PREFIX + "{}: {} partidos a procesar", leagueName, matchUrls.size());

        Set<String> existingMatchIds = matchStatsRepository.getExistingMatchIds();
        int total = 0;
        for (String matchUrl : matchUrls) {
            List<PlayerMatchStats> matchStats =
                whoScoredScraperService.scrapeMatchPlayerStats(matchUrl, existingMatchIds);
            if (!matchStats.isEmpty()) {
                matchStatsRepository.saveAllIfNew(matchStats);
                total += matchStats.size();
                log.info(LOG_PREFIX + "{} partido {}: {} stats guardados (total acum: {})",
                    leagueName,
                    matchUrl.replaceAll(".*/matches/(\\d+)/.*", "$1"),
                    matchStats.size(), total);
                matchStats.forEach(s -> existingMatchIds.add(s.getMatchId()));
            }
        }

        log.info(LOG_PREFIX + "{}: {} stats nuevos guardados", leagueName, total);
        return List.of();
    }

    public List<Player> scrapePlayersFromWhoScored(String leagueName) {
        List<Team> teams = teamRepository.findByLeague(leagueName);
        if (teams.isEmpty()) {
            throw new IllegalArgumentException("No hay equipos guardados para la liga: " + leagueName +
                ". Ejecutá primero GET /api/players/team-urls/" + leagueName);
        }
        List<Player> players = whoScoredScraperService.scrapePlayersFromTeams(teams);
        if (!players.isEmpty()) {
            repository.savePlayersForLeague(leagueName, players);
        }
        return players;
    }
}
