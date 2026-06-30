package com.futbol.tokenmarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Servicio scheduler de scraping")
class ScraperSchedulerServiceTest {

    @Mock private ScraperTriggerService scraperTriggerService;

    private ScraperSchedulerService scraperSchedulerService;

    @BeforeEach
    void setUp() {
        scraperSchedulerService = new ScraperSchedulerService(scraperTriggerService);
    }

    @Test
    @DisplayName("dispara el scraping de jugadores para todas las ligas")
    void triggersPlayersScrapeForAllLeagues() {
        scraperSchedulerService.scrapePlayersWeekly();

        verify(scraperTriggerService).triggerPlayersScrape("Premier League");
        verify(scraperTriggerService).triggerPlayersScrape("La Liga");
        verify(scraperTriggerService).triggerPlayersScrape("Serie A");
        verify(scraperTriggerService).triggerPlayersScrape("Bundesliga");
        verify(scraperTriggerService).triggerPlayersScrape("Ligue 1");
    }

    @Test
    @DisplayName("dispara el scraping por jornada para todas las ligas")
    void triggersMatchStatsScrapeForAllLeagues() {
        scraperSchedulerService.scrapeMatchStatsTwiceWeekly();

        verify(scraperTriggerService).triggerMatchStatsByMatchday("Premier League");
        verify(scraperTriggerService).triggerMatchStatsByMatchday("La Liga");
        verify(scraperTriggerService).triggerMatchStatsByMatchday("Serie A");
        verify(scraperTriggerService).triggerMatchStatsByMatchday("Bundesliga");
        verify(scraperTriggerService).triggerMatchStatsByMatchday("Ligue 1");
    }
}