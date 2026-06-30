package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.service.DataSeederService;
import com.futbol.tokenmarket.service.PlayerService;
import com.futbol.tokenmarket.service.QuoteService;
import com.futbol.tokenmarket.service.ScraperTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Controlador admin")
class AdminControllerTest {

    @Mock private ScraperTriggerService scraperTriggerService;
    @Mock private PlayerService playerService;
    @Mock private QuoteService quoteService;
    @Mock private DataSeederService dataSeederService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(scraperTriggerService, playerService, quoteService, dataSeederService);
    }

    @Nested
    @DisplayName("scrapeAllMatchStats")
    class ScrapeAllMatchStats {

        @Test
        @DisplayName("inicia el scraping para todas las ligas")
        void triggersScrapingForAllLeagues() {
            var result = adminController.scrapeAllMatchStats();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(scraperTriggerService).triggerMatchStatsByMatchdaySync("Premier League");
            verify(scraperTriggerService).triggerMatchStatsByMatchdaySync("La Liga");
            verify(scraperTriggerService).triggerMatchStatsByMatchdaySync("Serie A");
            verify(scraperTriggerService).triggerMatchStatsByMatchdaySync("Bundesliga");
            verify(scraperTriggerService).triggerMatchStatsByMatchdaySync("Ligue 1");
        }
    }

    @Nested
    @DisplayName("getTeamUrlsAdmin")
    class GetTeamUrlsAdmin {

        @Test
        @DisplayName("devuelve 200 cuando el scrapeo de equipos es exitoso")
        void returnsOkWhenScrapeSucceeds() {
            Team team = new Team("Barcelona", "https://example.com/barcelona", "La Liga");
            when(playerService.scrapeTeamUrls("La Liga")).thenReturn(List.of(team));

            var result = adminController.getTeamUrlsAdmin("La Liga");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsExactly(team);
        }

        @Test
        @DisplayName("devuelve 400 cuando la liga no es valida")
        void returnsBadRequestWhenLeagueIsInvalid() {
            when(playerService.scrapeTeamUrls("Inexistente")).thenThrow(new IllegalArgumentException("liga invalida"));

            var result = adminController.getTeamUrlsAdmin("Inexistente");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("devuelve 500 cuando falla el scraping")
        void returnsInternalServerErrorWhenScrapeFails() {
            when(playerService.scrapeTeamUrls("La Liga")).thenThrow(new RuntimeException("boom"));

            var result = adminController.getTeamUrlsAdmin("La Liga");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("scrapePlayersFromWhoScoredAdmin")
    class ScrapePlayersFromWhoScoredAdmin {

        @Test
        @DisplayName("devuelve 202 y delega al trigger service")
        void returnsAcceptedAndDelegatesToTriggerService() {
            var result = adminController.scrapePlayersFromWhoScoredAdmin("Premier League");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(scraperTriggerService).triggerPlayersScrape("Premier League");
        }
    }

    @Nested
    @DisplayName("recalculateQuotes")
    class RecalculateQuotes {

        @Test
        @DisplayName("recalcula cotizaciones y devuelve la primera pagina")
        void recalculatesQuotes() {
            PlayerQuote quote = new PlayerQuote();
            quote.setValue(111.0);
            when(quoteService.recalculateQuotes("balanced", 0, 10)).thenReturn(new PageImpl<>(List.of(quote)));

            var result = adminController.recalculateQuotes("balanced", 0, 10);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getContent()).containsExactly(quote);
        }
    }
}