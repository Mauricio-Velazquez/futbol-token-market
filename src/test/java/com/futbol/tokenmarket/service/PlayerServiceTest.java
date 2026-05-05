package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.repository.PlayerMatchStatsRepository;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService")
class PlayerServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerMatchStatsRepository matchStatsRepository;
    @Mock private WhoScoredScraperService whoScoredScraperService;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(
            playerRepository, teamRepository, matchStatsRepository,
            whoScoredScraperService
        );
    }

    @Nested
    @DisplayName("getLeagueStats")
    class GetLeagueStats {

        @Test
        @DisplayName("agrupa los jugadores por liga y cuenta correctamente")
        void groupsPlayersByLeagueAndCountsThem() throws IOException {
            List<Player> players = List.of(
                playerWithLeague("Premier League"),
                playerWithLeague("Premier League"),
                playerWithLeague("La Liga")
            );
            when(playerRepository.findAll()).thenReturn(players);

            List<LeagueStats> stats = playerService.getLeagueStats();

            assertThat(stats).hasSize(2);
            LeagueStats premierStats = stats.stream()
                .filter(s -> s.getLeague().equals("Premier League"))
                .findFirst().orElseThrow();
            assertThat(premierStats.getTotalPlayers()).isEqualTo(2);
        }

        @Test
        @DisplayName("devuelve las ligas ordenadas alfabéticamente")
        void returnsLeaguesSortedAlphabetically() throws IOException {
            List<Player> players = List.of(
                playerWithLeague("Serie A"),
                playerWithLeague("Bundesliga"),
                playerWithLeague("La Liga")
            );
            when(playerRepository.findAll()).thenReturn(players);

            List<LeagueStats> stats = playerService.getLeagueStats();

            assertThat(stats)
                .extracting(LeagueStats::getLeague)
                .containsExactly("Bundesliga", "La Liga", "Serie A");
        }

        @Test
        @DisplayName("devuelve lista vacía cuando no hay jugadores")
        void returnsEmptyListWhenNoPlayersExist() throws IOException {
            when(playerRepository.findAll()).thenReturn(List.of());

            List<LeagueStats> stats = playerService.getLeagueStats();

            assertThat(stats).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllPlayers")
    class GetAllPlayers {

        @Test
        @DisplayName("delega la consulta al repositorio sin transformaciones")
        void delegatesToRepositoryWithoutTransformation() throws IOException {
            List<Player> expected = List.of(playerWithLeague("La Liga"));
            when(playerRepository.findAll()).thenReturn(expected);

            List<Player> result = playerService.getAllPlayers();

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("getPlayerById")
    class GetPlayerById {

        @Test
        @DisplayName("devuelve el jugador cuando existe")
        void returnsPlayerWhenFound() throws IOException {
            Player player = playerWithLeague("La Liga");
            player.setId("p1");
            when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

            Optional<Player> result = playerService.getPlayerById("p1");

            assertThat(result).contains(player);
        }

        @Test
        @DisplayName("devuelve vacío cuando el jugador no existe")
        void returnsEmptyWhenPlayerNotFound() throws IOException {
            when(playerRepository.findById("inexistente")).thenReturn(Optional.empty());

            Optional<Player> result = playerService.getPlayerById("inexistente");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("scrapePlayersFromWhoScored")
    class ScrapePlayersFromWhoScored {

        @Test
        @DisplayName("lanza excepción cuando no hay equipos registrados para la liga")
        void throwsExceptionWhenNoTeamsRegisteredForLeague() throws IOException {
            when(teamRepository.findByLeague("Ligue 1")).thenReturn(List.of());

            assertThatThrownBy(() -> playerService.scrapePlayersFromWhoScored("Ligue 1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No hay equipos guardados para la liga: Ligue 1");
        }
    }

    // --- helpers ---

    private Player playerWithLeague(String league) {
        Player p = new Player();
        p.setLeague(league);
        return p;
    }
}
