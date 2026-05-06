package com.futbol.tokenmarket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScraperSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ScraperSchedulerService.class);

    private static final List<String> LEAGUES = List.of(
        "Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1"
    );

    private final ScraperTriggerService scraperTrigger;

    public ScraperSchedulerService(ScraperTriggerService scraperTrigger) {
        this.scraperTrigger = scraperTrigger;
    }

    @Scheduled(cron = "0 0 2 * * MON")
    public void scrapePlayersWeekly() {
        log.info("[Scheduler] Iniciando scraping de jugadores (lunes)");
        LEAGUES.forEach(scraperTrigger::triggerPlayersScrape);
    }

    @Scheduled(cron = "0 0 3 * * MON,FRI")
    public void scrapeMatchStatsTwiceWeekly() {
        log.info("[Scheduler] Iniciando scraping de partidos por jornada (lunes/viernes)");
        LEAGUES.forEach(scraperTrigger::triggerMatchStatsByMatchday);
    }
}
