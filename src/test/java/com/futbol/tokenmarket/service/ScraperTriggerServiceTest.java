package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Servicio disparador de scraping")
class ScraperTriggerServiceTest {

    @Mock private PlayerService playerService;

    private ScraperTriggerService scraperTriggerService;

    @BeforeEach
    void setUp() {
        scraperTriggerService = new ScraperTriggerService(playerService);
    }

    @Test
    @DisplayName("delega el scraping de jugadores cuando no falla")
    void triggersPlayersScrape() throws Exception {
        when(playerService.scrapePlayersFromWhoScored("La Liga")).thenReturn(List.of(new Player()));

        scraperTriggerService.triggerPlayersScrape("La Liga");

        verify(playerService).scrapePlayersFromWhoScored("La Liga");
    }

    @Test
    @DisplayName("absorbe errores al disparar scraping de jugadores")
    void swallowsErrorsWhenTriggeringPlayersScrape() throws Exception {
        when(playerService.scrapePlayersFromWhoScored("La Liga")).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> scraperTriggerService.triggerPlayersScrape("La Liga"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("delega el scraping por jornada cuando no falla")
    void triggersMatchStatsScrape() throws Exception {
        scraperTriggerService.triggerMatchStatsByMatchday("Serie A");

        verify(playerService).scrapeMatchStatsByMatchday("Serie A");
    }

    @Test
    @DisplayName("absorbe errores al disparar scraping por jornada")
    void swallowsErrorsWhenTriggeringMatchStatsScrape() throws Exception {
        when(playerService.scrapeMatchStatsByMatchday("Serie A")).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> scraperTriggerService.triggerMatchStatsByMatchday("Serie A"))
            .doesNotThrowAnyException();
    }
}