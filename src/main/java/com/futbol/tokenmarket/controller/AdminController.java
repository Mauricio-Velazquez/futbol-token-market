package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.service.PlayerService;
import com.futbol.tokenmarket.service.QuoteService;
import com.futbol.tokenmarket.service.ScraperTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final QuoteService quoteService;

    public AdminController(ScraperTriggerService scraperTrigger, PlayerService playerService, QuoteService quoteService) {
        this.scraperTrigger = scraperTrigger;
        this.playerService = playerService;
        this.quoteService = quoteService;
    }

    @PostMapping("/scrape/match-stats")
    @Operation(summary = "Scrapear estadísticas de jugadores de la última jornada (todas las ligas)",
               description = "Obtiene los partidos de la última jornada de cada liga y extrae stats de todos sus jugadores. Equivalente al job de lunes/viernes 03:00.")
    public ResponseEntity<String> scrapeAllMatchStats() {
        LEAGUES.forEach(scraperTrigger::triggerMatchStatsByMatchdaySync);
        return ResponseEntity.ok("Scraping por jornada completado para: " + LEAGUES);
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

    @PostMapping("/quotes/recalculate")
    @Operation(summary = "Recalcular cotizaciones para todos los jugadores",
               description = "Calcula cotizaciones con la estrategia seleccionada. Estrategias disponibles: balanced = estrategia equilibrada basada en rendimiento general; position-aware = estrategia por posicion que pondera de forma distinta segun el rol del jugador.")
    public ResponseEntity<Page<PlayerQuote>> recalculateQuotes(
            @Parameter(description = "Estrategia opcional: balanced (equilibrada) o position-aware (segun posicion)", example = "position-aware")
            @RequestParam(required = false) String strategy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(quoteService.recalculateQuotes(strategy, page, size));
    }
}
