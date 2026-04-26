package com.futbol.tokenmarket.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScraperSchedulerService {

    private static final List<String> LEAGUES = List.of(
        "Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1"
    );

    private final PlayerService playerService;

    public ScraperSchedulerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    // Lunes a las 02:00 — actualiza rosters (traspasos, nuevos jugadores)
    @Scheduled(cron = "0 0 2 * * MON")
    public void scrapePlayersWeekly() {
        System.out.println("[Scheduler] Iniciando scraping de jugadores (lunes)");
        for (String league : LEAGUES) {
            try {
                var players = playerService.scrapePlayersFromWhoScored(league);
                System.out.println("[Scheduler] " + league + ": " + players.size() + " jugadores actualizados");
            } catch (Exception e) {
                System.err.println("[Scheduler] Error scraping jugadores de " + league + ": " + e.getMessage());
            }
        }
        System.out.println("[Scheduler] Scraping de jugadores finalizado");
    }

    // Lunes y Viernes a las 03:00 — captura partidos nuevos desde la última ejecución
    @Scheduled(cron = "0 0 3 * * MON,FRI")
    public void scrapeMatchStatsTwiceWeekly() {
        System.out.println("[Scheduler] Iniciando scraping de partidos (lunes/viernes)");
        for (String league : LEAGUES) {
            try {
                var stats = playerService.scrapeMatchStatsForLeague(league);
                System.out.println("[Scheduler] " + league + ": " + stats.size() + " partidos nuevos");
            } catch (Exception e) {
                System.err.println("[Scheduler] Error scraping partidos de " + league + ": " + e.getMessage());
            }
        }
        System.out.println("[Scheduler] Scraping de partidos finalizado");
    }
}
