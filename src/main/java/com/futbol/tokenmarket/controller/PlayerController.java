package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.futbol.tokenmarket.model.Team;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "API para gestionar jugadores desde WhoScored")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping("/league-stats")
    @Operation(summary = "Obtener estadísticas de jugadores por liga",
               description = "Devuelve el total de jugadores cargados en la base de datos agrupados por liga")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LeagueStats.class))),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<List<LeagueStats>> getLeagueStats() {
        try {
            List<LeagueStats> stats = service.getLeagueStats();
            return ResponseEntity.ok(stats);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    @Operation(summary = "Obtener todos los jugadores",
               description = "Devuelve la lista completa de todos los jugadores cargados en la base de datos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<List<Player>> getAllPlayers() {
        try {
            return ResponseEntity.ok(service.getAllPlayers());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/league/{league}")
    @Operation(summary = "Obtener jugadores de una liga específica",
               description = "Devuelve todos los jugadores que pertenecen a una liga específica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<List<Player>> getPlayersByLeague(
            @Parameter(description = "Nombre de la liga", example = "Premier League", required = true)
            @PathVariable String league) {
        try {
            return ResponseEntity.ok(service.getPlayersByLeague(league));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/team-urls/{league}")
    @Operation(summary = "Obtener URLs de equipos desde WhoScored",
               description = "Scrapea WhoScored con Selenium para obtener los URLs de todos los equipos de una liga. Ligas soportadas: Premier League, La Liga, Serie A, Bundesliga, Ligue 1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "URLs obtenidos exitosamente"),
            @ApiResponse(responseCode = "400", description = "Liga no soportada"),
            @ApiResponse(responseCode = "500", description = "Error en el scraping")
    })
    public ResponseEntity<List<Team>> getTeamUrls(
            @Parameter(description = "Nombre de la liga", example = "Bundesliga", required = true)
            @PathVariable String league) {
        try {
            List<Team> teams = service.scrapeTeamUrls(league);
            return ResponseEntity.ok(teams);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @PostMapping("/scrape-from-whoscored/{league}")
    @Operation(summary = "Scrapear jugadores de WhoScored",
               description = "Para cada equipo guardado en la BD de la liga indicada, navega a su página en WhoScored y extrae los jugadores con sus estadísticas (goles, asistencias, rating, etc.). Requiere haber ejecutado primero GET /api/players/team-urls/{league}.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores scrapeados y guardados exitosamente"),
            @ApiResponse(responseCode = "400", description = "No hay equipos guardados para esa liga"),
            @ApiResponse(responseCode = "500", description = "Error en el scraping")
    })
    public ResponseEntity<LoadLeagueResponse> scrapePlayersFromWhoScored(
            @Parameter(description = "Nombre de la liga", example = "Bundesliga", required = true)
            @PathVariable String league) {
        try {
            List<Player> players = service.scrapePlayersFromWhoScored(league);
            return ResponseEntity.ok(new LoadLeagueResponse(
                    "Jugadores scrapeados exitosamente desde WhoScored",
                    league,
                    players.size(),
                    players
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new LoadLeagueResponse("Error: " + e.getMessage(), league, 0, null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new LoadLeagueResponse("Error en el scraping: " + e.getMessage(), league, 0, null));
        }
    }

    @PostMapping("/scrape-match-stats/{league}")
    @Operation(summary = "Scrapear estadísticas partido a partido",
               description = "Para cada jugador de la liga, navega a su página de Match Statistics en WhoScored y extrae los partidos que aún no están en la BD.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas scrapeadas exitosamente"),
            @ApiResponse(responseCode = "400", description = "No hay jugadores guardados para esa liga"),
            @ApiResponse(responseCode = "500", description = "Error en el scraping")
    })
    public ResponseEntity<MatchStatsResponse> scrapeMatchStats(
            @Parameter(description = "Nombre de la liga", example = "Bundesliga", required = true)
            @PathVariable String league) {
        try {
            List<PlayerMatchStats> newStats = service.scrapeMatchStatsForLeague(league);
            return ResponseEntity.ok(new MatchStatsResponse(
                    "Estadísticas scrapeadas exitosamente",
                    league,
                    newStats.size(),
                    newStats
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MatchStatsResponse("Error: " + e.getMessage(), league, 0, null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new MatchStatsResponse("Error: " + e.getMessage(), league, 0, null));
        }
    }

    public static class MatchStatsResponse {
        public String message;
        public String league;
        public Integer newRecords;
        public List<PlayerMatchStats> stats;

        public MatchStatsResponse(String message, String league, Integer newRecords, List<PlayerMatchStats> stats) {
            this.message = message;
            this.league = league;
            this.newRecords = newRecords;
            this.stats = stats;
        }
    }

    // DTO para la respuesta de carga de liga
    public static class LoadLeagueResponse {
        public String message;
        public String league;
        public Integer playersLoaded;
        public List<Player> players;

        public LoadLeagueResponse(String message, String league, Integer playersLoaded, List<Player> players) {
            this.message = message;
            this.league = league;
            this.playersLoaded = playersLoaded;
            this.players = players;
        }

        public String getMessage() { return message; }
        public String getLeague() { return league; }
        public Integer getPlayersLoaded() { return playersLoaded; }
        public List<Player> getPlayers() { return players; }
    }
}
