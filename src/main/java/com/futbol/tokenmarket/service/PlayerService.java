package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.repository.PlayerMatchStatsRepository;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {

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

    public List<LeagueStats> getLeagueStats() throws IOException {
        List<Player> allPlayers = repository.findAll();

        return allPlayers.stream()
                .collect(Collectors.groupingBy(Player::getLeague, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new LeagueStats(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(LeagueStats::getLeague))
                .toList();
    }

    public List<Player> getAllPlayers() throws IOException {
        return repository.findAll();
    }

    public List<Player> getPlayersByLeague(String league) throws IOException {
        return repository.findByLeague(league);
    }

    public List<Player> getFilteredPlayers(String league, String team, String position) throws IOException {
        return repository.findByFilters(league, team, position);
    }

    public Optional<Player> getPlayerById(String id) throws IOException {
        return repository.findById(id);
    }

    public List<Team> scrapeTeamUrls(String leagueName) throws IOException {
        Map<String, String> scraped = whoScoredScraperService.scrapeTeamUrls(leagueName);

        List<Team> teams = scraped.entrySet().stream()
                .map(e -> new Team(e.getKey(), e.getValue(), leagueName))
                .toList();

        return teamRepository.saveTeamsForLeague(leagueName, teams);
    }

    public List<PlayerMatchStats> scrapeMatchStatsByMatchday(String leagueName) throws IOException {
        List<String> matchUrls = whoScoredScraperService.scrapeLeagueMatchUrls(leagueName);
        if (matchUrls.isEmpty()) {
            System.out.println("[PlayerService] " + leagueName + ": no se encontraron partidos en la página de la liga");
            return List.of();
        }
        System.out.println("[PlayerService] " + leagueName + ": " + matchUrls.size() + " partidos a procesar");

        Set<String> existingMatchIds = matchStatsRepository.getExistingMatchIds();

        File tempFile = matchStatsRepository.createLeagueTempFile(leagueName);
        int total = 0;
        boolean committed = false;
        try {
            for (String matchUrl : matchUrls) {
                List<PlayerMatchStats> matchStats =
                    whoScoredScraperService.scrapeMatchPlayerStats(matchUrl, existingMatchIds);
                if (!matchStats.isEmpty()) {
                    matchStatsRepository.appendToTemp(matchStats, tempFile);
                    total += matchStats.size();
                    System.out.println("[PlayerService] " + leagueName + " partido " +
                        matchUrl.replaceAll(".*/matches/(\\d+)/.*", "$1") +
                        ": " + matchStats.size() + " stats guardados (total acum: " + total + ")");
                }
            }
            matchStatsRepository.commitAndDeleteTemp(tempFile);
            committed = true;
        } finally {
            if (!committed) {
                try {
                    matchStatsRepository.commitAndDeleteTemp(tempFile);
                    System.out.println("[PlayerService] " + leagueName + ": guardado parcial por fallo");
                } catch (Exception e) {
                    if (tempFile.exists()) tempFile.delete();
                }
            }
        }

        System.out.println("[PlayerService] " + leagueName + ": " + total + " stats nuevos guardados");
        return List.of();
    }

    public List<Player> scrapePlayersFromWhoScored(String leagueName) throws IOException {
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
