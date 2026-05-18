package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Player;
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

import java.util.List;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Players", description = "API para gestionar jugadores desde WhoScored")
public class PlayerController {

    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Obtener jugadores con filtros opcionales",
               description = "Devuelve jugadores filtrados por liga, equipo y/o posición. Posiciones válidas: GK, D, DM, M, AM, FW")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Jugadores obtenidos exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Player.class))),
            @ApiResponse(responseCode = "500", description = "Error en el servidor")
    })
    public ResponseEntity<List<Player>> getAllPlayers(
            @Parameter(description = "Filtrar por liga", example = "Bundesliga")
            @RequestParam(required = false) String league,
            @Parameter(description = "Filtrar por equipo", example = "Bayern Munich")
            @RequestParam(required = false) String team,
            @Parameter(description = "Filtrar por posición: GK, D, DM, M, AM, FW", example = "FW")
            @RequestParam(required = false) String position) {
        return ResponseEntity.ok(service.getFilteredPlayers(league, team, position));
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
}
