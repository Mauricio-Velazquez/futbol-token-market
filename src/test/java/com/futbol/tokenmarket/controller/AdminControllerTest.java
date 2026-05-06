package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.service.PlayerService;
import com.futbol.tokenmarket.service.ScraperTriggerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador admin")
class AdminControllerTest {

    @Mock private ScraperTriggerService scraperTriggerService;
    @Mock private PlayerService playerService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(scraperTriggerService, playerService);
    }

    @Nested
    @DisplayName("scrapeAllMatchStats")
    class ScrapeAllMatchStats {

        @Test
        @DisplayName("inicia el scraping para todas las ligas")
        void triggersScrapingForAllLeagues() {
            var result = adminController.scrapeAllMatchStats();

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            verify(scraperTriggerService).triggerMatchStatsByMatchday("Premier League");
            verify(scraperTriggerService).triggerMatchStatsByMatchday("La Liga");
            verify(scraperTriggerService).triggerMatchStatsByMatchday("Serie A");
            verify(scraperTriggerService).triggerMatchStatsByMatchday("Bundesliga");
            verify(scraperTriggerService).triggerMatchStatsByMatchday("Ligue 1");
        }
    }

    @Nested
    @DisplayName("getTeamUrlsAdmin")
    class GetTeamUrlsAdmin {

        @Test
        @DisplayName("devuelve 200 cuando el scrapeo de equipos es exitoso")
        void returnsOkWhenScrapeSucceeds() throws IOException {
            Team team = new Team("Barcelona", "https://example.com/barcelona", "La Liga");
            when(playerService.scrapeTeamUrls("La Liga")).thenReturn(List.of(team));

            var result = adminController.getTeamUrlsAdmin("La Liga");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsExactly(team);
        }

        @Test
        @DisplayName("devuelve 400 cuando la liga no es valida")
        void returnsBadRequestWhenLeagueIsInvalid() throws IOException {
            when(playerService.scrapeTeamUrls("Inexistente")).thenThrow(new IllegalArgumentException("liga invalida"));

            var result = adminController.getTeamUrlsAdmin("Inexistente");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("devuelve 500 cuando falla el scraping")
        void returnsInternalServerErrorWhenScrapeFails() throws IOException {
            when(playerService.scrapeTeamUrls("La Liga")).thenThrow(new IOException("boom"));

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
}