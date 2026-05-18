package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.service.PlayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de estadisticas de jugadores")
class AdminPlayerStatsControllerTest {

    @Mock private PlayerService playerService;

    private AdminPlayerStatsController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminPlayerStatsController(playerService);
    }

    @Test
    @DisplayName("devuelve 200 con las estadisticas de liga")
    void returnsLeagueStats() {
        List<LeagueStats> stats = List.of(new LeagueStats("La Liga", 3));
        when(playerService.getLeagueStats()).thenReturn(stats);

        var result = controller.getLeagueStats();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(stats);
    }

    @Test
    @DisplayName("devuelve 500 si el servicio falla")
    void returnsInternalServerErrorWhenServiceFails() {
        when(playerService.getLeagueStats()).thenThrow(new RuntimeException("boom"));

        var result = controller.getLeagueStats();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}