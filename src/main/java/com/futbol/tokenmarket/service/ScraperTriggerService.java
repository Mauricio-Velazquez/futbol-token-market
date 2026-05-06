package com.futbol.tokenmarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ScraperTriggerService {

    private static final Logger log = LoggerFactory.getLogger(ScraperTriggerService.class);

    private final PlayerService playerService;

    public ScraperTriggerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Async
    public void triggerPlayersScrape(String league) {
        log.info("[Scraper] Iniciando scraping de jugadores: {}", league);
        try {
            var players = playerService.scrapePlayersFromWhoScored(league);
            log.info("[Scraper] {}: {} jugadores actualizados", league, players.size());
        } catch (Exception e) {
            log.error("[Scraper] Error scraping jugadores de {}: {}", league, e.getMessage(), e);
        }
    }

    @Async
    public void triggerMatchStatsByMatchday(String league) {
        log.info("[Scraper] Iniciando scraping por jornada: {}", league);
        try {
            playerService.scrapeMatchStatsByMatchday(league);
            log.info("[Scraper] {}: scraping por jornada completado", league);
        } catch (Exception e) {
            log.error("[Scraper] Error scraping jornada de {}: {}", league, e.getMessage(), e);
        }
    }
}
