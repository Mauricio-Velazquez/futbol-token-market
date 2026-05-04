package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/players")
@Tag(name = "Admin", description = "Operaciones administrativas")
public class AdminPlayerStatsController {

    private final PlayerService playerService;

    public AdminPlayerStatsController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/league-stats")
    @Operation(summary = "Obtener cantidad de jugadores cargados en cada liga",
               description = "Devuelve el total de jugadores cargados en la base de datos agrupados por liga.")
    public ResponseEntity<List<LeagueStats>> getLeagueStats() {
        try {
            return ResponseEntity.ok(playerService.getLeagueStats());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}