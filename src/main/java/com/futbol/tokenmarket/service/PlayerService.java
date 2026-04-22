package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    private final PlayerRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WhoScoredScraperService whoScoredScraperService;
    private final String apiToken;
    private final String apiBaseUrl = "https://api.football-data.org/v4";

    public PlayerService(PlayerRepository repository, RestTemplate restTemplate, ObjectMapper objectMapper,
                         WhoScoredScraperService whoScoredScraperService,
                         @Value("${football.data.api.token}") String apiToken) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.whoScoredScraperService = whoScoredScraperService;
        this.apiToken = apiToken;
    }

    public List<Player> loadPlayersFromLeague(String leagueName) throws IOException {
        String competitionId = getCompetitionIdByName(leagueName);
        if (competitionId == null) {
            throw new IllegalArgumentException("Liga no encontrada: " + leagueName);
        }

        String url = apiBaseUrl + "/competitions/" + competitionId + "/teams";
        String response = restTemplate.getForObject(url, String.class);
        
        List<Player> playersToSave = new ArrayList<>();
        JsonNode rootNode = objectMapper.readTree(response);
        JsonNode teamsNode = rootNode.get("teams");

        if (teamsNode != null && teamsNode.isArray()) {
            for (JsonNode team : teamsNode) {
                String teamName = team.get("name").asText();
                String teamId = team.get("id").asText();
                JsonNode squareNode = team.get("squad");

                if (squareNode != null && squareNode.isArray()) {
                    for (JsonNode player : squareNode) {
                        Player p = new Player();
                        p.setId(player.get("id").asText());
                        p.setName(player.get("name").asText());
                        p.setPosition(player.get("position") != null ? player.get("position").asText() : "Unknown");
                        p.setJerseyNumber(player.get("shirtNumber") != null ? player.get("shirtNumber").asInt() : null);
                        p.setLeague(leagueName);
                        p.setTeam(teamName);
                        p.setNationality(player.get("nationality") != null ? player.get("nationality").asText() : "Unknown");
                        p.setDateOfBirth(player.get("dateOfBirth") != null ? player.get("dateOfBirth").hashCode() : null);

                        playersToSave.add(p);
                    }
                }
            }
        }

        if (!playersToSave.isEmpty()) {
            repository.savePlayersForLeague(leagueName, playersToSave);
        }

        return playersToSave;
    }

    public List<LeagueStats> getLeagueStats() throws IOException {
        List<Player> allPlayers = repository.findAll();
        
        return allPlayers.stream()
                .collect(Collectors.groupingBy(Player::getLeague, Collectors.counting()))
                .entrySet()
                .stream()
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

    public List<Player> enrichPlayersWithWhoScoredStats(String league) throws IOException {
        List<Player> playersToEnrich = repository.findByLeague(league);
        
        for (Player player : playersToEnrich) {
            whoScoredScraperService.enrichPlayerWithStats(player);
        }
        
        // Guardar los jugadores enriquecidos
        repository.savePlayersForLeague(league, playersToEnrich);
        
        return playersToEnrich;
    }

    private String getCompetitionIdByName(String leagueName) {
        Map<String, String> leagueMap = new HashMap<>();
        leagueMap.put("Premier League", "PL");
        leagueMap.put("La Liga", "SA");
        leagueMap.put("Serie A", "SA");
        leagueMap.put("Bundesliga", "BL1");
        leagueMap.put("Ligue 1", "FL1");
        leagueMap.put("Champions League", "CL");
        leagueMap.put("Europa League", "EL");

        return leagueMap.get(leagueName);
    }
}
