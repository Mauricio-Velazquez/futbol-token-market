package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.service.PlayerService;
import com.futbol.tokenmarket.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "API para gestionar jugadores desde WhoScored")
public class PlayerController {

    private final PlayerService service;
        private final QuoteService quoteService;

        public PlayerController(PlayerService service, QuoteService quoteService) {
        this.service = service;
                this.quoteService = quoteService;
    }

    @GetMapping
    @Operation(summary = "Obtener jugadores con filtros opcionales",
                           description = "Devuelve jugadores filtrados por liga, equipo y/o posición. Posiciones válidas: GK, D, DM, M, AM, FW. La respuesta está paginada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
        public ResponseEntity<Page<Player>> getAllPlayers(
            @Parameter(description = "Filtrar por liga", example = "Bundesliga")
            @RequestParam(required = false) String league,
            @Parameter(description = "Filtrar por equipo", example = "Bayern Munich")
            @RequestParam(required = false) String team,
            @Parameter(description = "Filtrar por posición: GK, D, DM, M, AM, FW", example = "FW")
                        @RequestParam(required = false) String position,
                        @Parameter(description = "Número de página", example = "0")
                        @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Tamaño de página", example = "10")
                        @RequestParam(defaultValue = "10") int size) {
                return ResponseEntity.ok(service.getFilteredPlayers(league, team, position, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un jugador por ID",
               description = "Devuelve el jugador con el ID especificado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugador encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "404", description = "Jugador no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<Player> getPlayerById(
            @Parameter(description = "ID del jugador", example = "ws_83532", required = true)
            @PathVariable String id) {
        return service.getPlayerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/quotes")
    @Operation(summary = "Historial de cotizaciones de un jugador",
               description = "Devuelve el historial paginado de cotizaciones del jugador solicitado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historial obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerQuote.class))),
            @ApiResponse(responseCode = "404", description = "Jugador no encontrado")
    })
    public ResponseEntity<Page<PlayerQuote>> getPlayerQuotes(
            @PathVariable String id,
            @Parameter(description = "Número de página", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Fecha de inicio del filtro (inclusive), formato yyyy-MM-dd", example = "2025-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Fecha de fin del filtro (inclusive), formato yyyy-MM-dd", example = "2025-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.getPlayerById(id)
            .map(player -> ResponseEntity.ok(quoteService.getPlayerQuoteHistory(player.getId(), from, to, page, size)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ranking")
    @Operation(summary = "Ranking de jugadores según estrategia activa",
               description = "Ordena a los jugadores por su cotización actual calculada con la estrategia activa. La respuesta está paginada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking obtenido exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PlayerQuote.class)))
    })
    public ResponseEntity<Page<PlayerQuote>> getRanking(
            @Parameter(description = "Número de página", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(quoteService.getRanking(page, size));
    }
}
