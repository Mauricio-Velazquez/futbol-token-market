package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.service.PlayerService;
import com.futbol.tokenmarket.service.QuoteService;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Controlador de jugadores")
class PlayerControllerTest {

    @Mock private PlayerService playerService;
    @Mock private QuoteService quoteService;

    private PlayerController playerController;

    @BeforeEach
    void setUp() {
        playerController = new PlayerController(playerService, quoteService);
    }

    @Nested
    @DisplayName("getAllPlayers")
    class GetAllPlayers {

        @Test
        @DisplayName("devuelve los jugadores filtrados y delega los filtros al servicio")
        void returnsFilteredPlayers() throws IOException {
            Player player = player("ws_1", "Jugador Uno");
            when(playerService.getFilteredPlayers("Bundesliga", "Bayern", "FW", 0, 10))
                .thenReturn(new PageImpl<>(List.of(player)));

            var result = playerController.getAllPlayers("Bundesliga", "Bayern", "FW", 0, 10);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getContent()).containsExactly(player);
            verify(playerService).getFilteredPlayers("Bundesliga", "Bayern", "FW", 0, 10);
        }
    }

    @Nested
    @DisplayName("getPlayerById")
    class GetPlayerById {

        @Test
        @DisplayName("devuelve 200 cuando el jugador existe")
        void returnsOkWhenPlayerExists() throws IOException {
            Player player = player("ws_2", "Jugador Dos");
            when(playerService.getPlayerById("ws_2")).thenReturn(Optional.of(player));

            var result = playerController.getPlayerById("ws_2");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(player);
            verify(playerService).getPlayerById("ws_2");
        }

        @Test
        @DisplayName("devuelve 404 cuando el jugador no existe")
        void returnsNotFoundWhenPlayerDoesNotExist() throws IOException {
            when(playerService.getPlayerById("inexistente")).thenReturn(Optional.empty());

            var result = playerController.getPlayerById("inexistente");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getPlayerQuotes")
    class GetPlayerQuotes {

        @Test
        @DisplayName("devuelve el historial paginado de cotizaciones")
        void returnsQuoteHistory() throws IOException {
            Player player = player("ws_3", "Jugador Tres");
            PlayerQuote quote = new PlayerQuote();
            quote.setValue(123.0);
            when(playerService.getPlayerById("ws_3")).thenReturn(Optional.of(player));
            when(quoteService.getPlayerQuoteHistory("ws_3", null, null, 0, 10)).thenReturn(new PageImpl<>(List.of(quote)));

            var result = playerController.getPlayerQuotes("ws_3", 0, 10, null, null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getContent()).containsExactly(quote);
        }
    }

    @Nested
    @DisplayName("getRanking")
    class GetRanking {

        @Test
        @DisplayName("devuelve el ranking paginado")
        void returnsRanking() {
            PlayerQuote quote = new PlayerQuote();
            quote.setValue(150.0);
            when(quoteService.getRanking(0, 10)).thenReturn(new PageImpl<>(List.of(quote)));

            var result = playerController.getRanking(0, 10);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getContent()).containsExactly(quote);
        }
    }

    private Player player(String id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        return player;
    }
}