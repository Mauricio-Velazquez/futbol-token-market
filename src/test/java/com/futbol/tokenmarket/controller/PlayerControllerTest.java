package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.service.PlayerService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de jugadores")
class PlayerControllerTest {

    @Mock private PlayerService playerService;

    private PlayerController playerController;

    @BeforeEach
    void setUp() {
        playerController = new PlayerController(playerService);
    }

    @Nested
    @DisplayName("getAllPlayers")
    class GetAllPlayers {

        @Test
        @DisplayName("devuelve los jugadores filtrados y delega los filtros al servicio")
        void returnsFilteredPlayers() throws IOException {
            Player player = player("ws_1", "Jugador Uno");
            when(playerService.getFilteredPlayers("Bundesliga", "Bayern", "FW"))
                .thenReturn(List.of(player));

            var result = playerController.getAllPlayers("Bundesliga", "Bayern", "FW");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).containsExactly(player);
            verify(playerService).getFilteredPlayers("Bundesliga", "Bayern", "FW");
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

    private Player player(String id, String name) {
        Player player = new Player();
        player.setId(id);
        player.setName(name);
        return player;
    }
}