package com.futbol.tokenmarket.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScraperSchedulerService {

    private static final List<String> LEAGUES = List.of(
        "Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1"
    );

    private final ScraperTriggerService scraperTrigger;

    public ScraperSchedulerService(ScraperTriggerService scraperTrigger) {
        this.scraperTrigger = scraperTrigger;
    }

    // Lunes a las 02:00 — actualiza rosters (traspasos, nuevos jugadores)
    @Scheduled(cron = "0 0 2 * * MON")
    public void scrapePlayersWeekly() {
        System.out.println("[Scheduler] Iniciando scraping de jugadores (lunes)");
        LEAGUES.forEach(scraperTrigger::triggerPlayersScrape);
    }

    // Lunes y Viernes a las 03:00 — captura partidos de la última jornada (enfoque match-centric)
    @Scheduled(cron = "0 0 3 * * MON,FRI")
    public void scrapeMatchStatsTwiceWeekly() {
        System.out.println("[Scheduler] Iniciando scraping de partidos por jornada (lunes/viernes)");
        LEAGUES.forEach(scraperTrigger::triggerMatchStatsByMatchday);
    }
}
