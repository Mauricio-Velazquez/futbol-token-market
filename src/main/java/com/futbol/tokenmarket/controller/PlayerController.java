package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.LeagueStats;
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

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "API para gestionar jugadores y cargarlos desde Football-Data.org")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @PostMapping("/load-league")
    @Operation(summary = "Cargar jugadores de una liga", 
               description = "Carga todos los jugadores de una liga específica desde Football-Data.org. Soporta: Premier League, La Liga, Serie A, Bundesliga, Ligue 1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores cargados exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "400", description = "Liga no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<LoadLeagueResponse> loadLeague(
            @Parameter(description = "Nombre de la liga (ej: Premier League, La Liga, Serie A, Bundesliga, Ligue 1)", example = "Premier League", required = true)
            @RequestParam String leagueName) {
        try {
            List<Player> loadedPlayers = service.loadPlayersFromLeague(leagueName);
            return ResponseEntity.ok(new LoadLeagueResponse(
                    "Jugadores cargados exitosamente",
                    leagueName,
                    loadedPlayers.size(),
                    loadedPlayers
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new LoadLeagueResponse("Error: " + e.getMessage(), leagueName, 0, null));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new LoadLeagueResponse("Error al cargar jugadores: " + e.getMessage(), leagueName, 0, null));
        }
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

    @PostMapping("/enrich-with-whoscored/{league}")
    @Operation(summary = "Enriquecer jugadores de una liga con estadísticas de WhoScored",
               description = "Obtiene datos de rendimiento (goles, asistencias, tiros, etc.) desde WhoScored para todos los jugadores de una liga y los almacena localmente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores enriquecidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "404", description = "Liga no encontrada"),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<EnrichLeagueResponse> enrichPlayersFromWhoScored(
            @Parameter(description = "Nombre de la liga", example = "Premier League", required = true)
            @PathVariable String league) {
        try {
            List<Player> enrichedPlayers = service.enrichPlayersWithWhoScoredStats(league);
            return ResponseEntity.ok(new EnrichLeagueResponse(
                    "Jugadores enriquecidos exitosamente con estadísticas de WhoScored",
                    league,
                    enrichedPlayers.size(),
                    enrichedPlayers
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new EnrichLeagueResponse("Error al enriquecer jugadores: " + e.getMessage(), league, 0, null));
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

    // DTO para la respuesta de enriquecimiento de liga
    public static class EnrichLeagueResponse {
        public String message;
        public String league;
        public Integer playersEnriched;
        public List<Player> players;

        public EnrichLeagueResponse(String message, String league, Integer playersEnriched, List<Player> players) {
            this.message = message;
            this.league = league;
            this.playersEnriched = playersEnriched;
            this.players = players;
        }

        public String getMessage() { return message; }
        public String getLeague() { return league; }
        public Integer getPlayersEnriched() { return playersEnriched; }
        public List<Player> getPlayers() { return players; }
    }
}
