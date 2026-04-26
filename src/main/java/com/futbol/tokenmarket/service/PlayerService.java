package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository repository;
    private final TeamRepository teamRepository;
    private final WhoScoredScraperService whoScoredScraperService;
    private final PlayerJsonService playerJsonService;

    public PlayerService(PlayerRepository repository,
                         TeamRepository teamRepository,
                         WhoScoredScraperService whoScoredScraperService,
                         PlayerJsonService playerJsonService) {
        this.repository = repository;
        this.teamRepository = teamRepository;
        this.whoScoredScraperService = whoScoredScraperService;
        this.playerJsonService = playerJsonService;
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

    public List<Team> scrapeTeamUrls(String leagueName) throws IOException {
        Map<String, String> scraped = whoScoredScraperService.scrapeTeamUrls(leagueName);

        List<Team> teams = scraped.entrySet().stream()
                .map(e -> new Team(e.getKey(), e.getValue(), leagueName))
                .toList();

        return teamRepository.saveTeamsForLeague(leagueName, teams);
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
            // Guardar también en JSON (agrega sin sobrescribir)
            playerJsonService.appendPlayersToJson(players);
            System.out.println("[PlayerService] Guardados " + players.size() + " jugadores en players.json");
        }
        return players;
    }
}
