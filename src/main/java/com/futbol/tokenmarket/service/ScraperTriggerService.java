package com.futbol.tokenmarket.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ScraperTriggerService {

    private final PlayerService playerService;

    public ScraperTriggerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Async
    public void triggerPlayersScrape(String league) {
        System.out.println("[Scraper] Iniciando scraping de jugadores: " + league);
        try {
            var players = playerService.scrapePlayersFromWhoScored(league);
            System.out.println("[Scraper] " + league + ": " + players.size() + " jugadores actualizados");
        } catch (Exception e) {
            System.err.println("[Scraper] Error scraping jugadores de " + league + ": " + e.getMessage());
        }
    }

    @Async
    public void triggerMatchStatsScrape(String league) {
        System.out.println("[Scraper] Iniciando scraping de partidos (legacy): " + league);
        try {
            playerService.scrapeMatchStatsForLeague(league);
        } catch (Exception e) {
            System.err.println("[Scraper] Error scraping partidos de " + league + ": " + e.getMessage());
        }
    }

    @Async
    public void triggerMatchStatsByMatchday(String league) {
        System.out.println("[Scraper] Iniciando scraping por jornada: " + league);
        try {
            playerService.scrapeMatchStatsByMatchday(league);
            System.out.println("[Scraper] " + league + ": scraping por jornada completado");
        } catch (Exception e) {
            System.err.println("[Scraper] Error scraping jornada de " + league + ": " + e.getMessage());
        }
    }
}
