package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/players")
@Tag(name = "Admin", description = "Operaciones administrativas")
public class AdminPlayerStatsController {

    private static final Logger log = LoggerFactory.getLogger(AdminPlayerStatsController.class);

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
        } catch (Exception e) {
            log.error("Error obteniendo stats de ligas: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
