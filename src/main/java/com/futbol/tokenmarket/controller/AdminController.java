package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.service.ScraperTriggerService;
import com.futbol.tokenmarket.service.PlayerService;
import com.futbol.tokenmarket.model.Team;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Operaciones administrativas")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final List<String> LEAGUES = List.of(
        "Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1"
    );

    private final ScraperTriggerService scraperTrigger;
    private final PlayerService playerService;

    public AdminController(ScraperTriggerService scraperTrigger, PlayerService playerService) {
        this.scraperTrigger = scraperTrigger;
        this.playerService = playerService;
    }

    @PostMapping("/scrape/match-stats")
    @Operation(summary = "Scrapear estadísticas de jugadores de la última jornada (todas las ligas)",
               description = "Obtiene los partidos de la última jornada de cada liga y extrae stats de todos sus jugadores. Equivalente al job de lunes/viernes 03:00.")
    public ResponseEntity<String> scrapeAllMatchStats() {
        LEAGUES.forEach(scraperTrigger::triggerMatchStatsByMatchday);
        return ResponseEntity.accepted().body("Scraping por jornada iniciado en background para: " + LEAGUES);
    }

    @GetMapping("/team-urls/{league}")
    @Operation(summary = "Obtener URLs de equipos desde WhoScored",
               description = "Scrapea WhoScored con Selenium para obtener los URLs de todos los equipos de una liga. Ligas soportadas: Premier League, La Liga, Serie A, Bundesliga, Ligue 1")
    public ResponseEntity<List<Team>> getTeamUrlsAdmin(
            @org.springframework.web.bind.annotation.PathVariable String league) {
        try {
            List<Team> teams = playerService.scrapeTeamUrls(league);
            return ResponseEntity.ok(teams);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error scrapeando equipos para liga {}: {}", league, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/scrape-from-whoscored/{league}")
    @Operation(summary = "Scrapear jugadores de WhoScored",
               description = "Para cada equipo guardado en la BD de la liga indicada, navega a su página en WhoScored y extrae nombre, posición y URL de cada jugador. Requiere haber ejecutado primero GET /api/admin/team-urls/{league}.")
    public ResponseEntity<String> scrapePlayersFromWhoScoredAdmin(
            @org.springframework.web.bind.annotation.PathVariable String league) {
        scraperTrigger.triggerPlayersScrape(league);
        return ResponseEntity.accepted().body("Scraping de jugadores iniciado en background para " + league);
    }
}
