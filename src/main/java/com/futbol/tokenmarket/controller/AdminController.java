package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.service.ScraperTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Operaciones administrativas")
public class AdminController {

    private static final List<String> LEAGUES = List.of(
        "Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1"
    );

    private final ScraperTriggerService scraperTrigger;

    public AdminController(ScraperTriggerService scraperTrigger) {
        this.scraperTrigger = scraperTrigger;
    }

    @PostMapping("/scrape/players")
    @Operation(summary = "Scrapear jugadores de todas las ligas",
               description = "Dispara el scraping de jugadores para las 5 ligas en background. Equivalente al job del lunes 02:00.")
    public ResponseEntity<String> scrapeAllPlayers() {
        LEAGUES.forEach(scraperTrigger::triggerPlayersScrape);
        return ResponseEntity.accepted().body("Scraping de jugadores iniciado en background para: " + LEAGUES);
    }

    @PostMapping("/scrape/match-stats")
    @Operation(summary = "Scrapear estadísticas de la última jornada (todas las ligas)",
               description = "Obtiene los partidos de la última jornada de cada liga y extrae stats de todos sus jugadores. Equivalente al job de lunes/viernes 03:00.")
    public ResponseEntity<String> scrapeAllMatchStats() {
        LEAGUES.forEach(scraperTrigger::triggerMatchStatsByMatchday);
        return ResponseEntity.accepted().body("Scraping por jornada iniciado en background para: " + LEAGUES);
    }

    @PostMapping("/scrape/match-stats/legacy")
    @Operation(summary = "Scrapear estadísticas (enfoque legacy por jugador)",
               description = "Recorre todos los jugadores individualmente. Muy lento, solo para backfill o debugging.")
    public ResponseEntity<String> scrapeAllMatchStatsLegacy() {
        LEAGUES.forEach(scraperTrigger::triggerMatchStatsScrape);
        return ResponseEntity.accepted().body("Scraping legacy iniciado en background para: " + LEAGUES);
    }
}
