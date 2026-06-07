package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.LeagueStats;
import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.repository.PlayerMatchStatsRepository;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.TeamRepository;
import com.futbol.tokenmarket.repository.TokenHoldingRepository;
import com.futbol.tokenmarket.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerService")
class PlayerServiceTest {

    @Mock private PlayerRepository playerRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private PlayerMatchStatsRepository matchStatsRepository;
    @Mock private WhoScoredScraperService whoScoredScraperService;
    @Mock private TokenHoldingRepository tokenHoldingRepository; 
    @Mock private UserRepository userRepository;                 

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(
            playerRepository, teamRepository, matchStatsRepository,
            whoScoredScraperService, tokenHoldingRepository, userRepository
        );
    }

    @Nested
    @DisplayName("getLeagueStats")
    class GetLeagueStats {

        @Test
        @DisplayName("agrupa los jugadores por liga y cuenta correctamente")
        void groupsPlayersByLeagueAndCountsThem() {
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
        void returnsLeaguesSortedAlphabetically() {
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
        void returnsEmptyListWhenNoPlayersExist() {
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
        void delegatesToRepositoryWithoutTransformation() {
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
        void returnsPlayerWhenFound() {
            Player player = playerWithLeague("La Liga");
            player.setId("p1");
            when(playerRepository.findById("p1")).thenReturn(Optional.of(player));

            Optional<Player> result = playerService.getPlayerById("p1");

            assertThat(result).contains(player);
        }

        @Test
        @DisplayName("devuelve vacío cuando el jugador no existe")
        void returnsEmptyWhenPlayerNotFound() {
            when(playerRepository.findById("inexistente")).thenReturn(Optional.empty());

            Optional<Player> result = playerService.getPlayerById("inexistente");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPlayersByLeague")
    class GetPlayersByLeague {

        @Test
        @DisplayName("delega al repositorio con la liga exacta")
        void delegatesToRepositoryWithExactLeague() {
            List<Player> expected = List.of(playerWithLeague("Bundesliga"));
            when(playerRepository.findByLeague("Bundesliga")).thenReturn(expected);

            List<Player> result = playerService.getPlayersByLeague("Bundesliga");

            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("getFilteredPlayers")
    class GetFilteredPlayers {

        @Test
        @DisplayName("delega los tres filtros al repositorio sin modificarlos")
        void passesAllThreeFiltersToRepository() {
            List<Player> expected = List.of(playerWithLeague("La Liga"));
            when(playerRepository.findByFilters("La Liga", "Barcelona", "GK")).thenReturn(expected);

            List<Player> result = playerService.getFilteredPlayers("La Liga", "Barcelona", "GK");

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("acepta filtros nulos y los pasa al repositorio tal cual")
        void passesNullFiltersToRepository() {
            when(playerRepository.findByFilters(null, null, null)).thenReturn(List.of());

            List<Player> result = playerService.getFilteredPlayers(null, null, null);

            assertThat(result).isEmpty();
            verify(playerRepository).findByFilters(null, null, null);
        }
    }

    @Nested
    @DisplayName("scrapeTeamUrls")
    class ScrapeTeamUrls {

        @Test
        @DisplayName("convierte el mapa del scraper en Team con la liga correcta")
        void mapsScrapedEntriesIntoTeamsWithCorrectLeague() {
            when(whoScoredScraperService.scrapeTeamUrls("La Liga")).thenReturn(
                java.util.Map.of("Barcelona", "https://whoscored.com/barcelona")
            );
            when(teamRepository.saveTeamsForLeague(eq("La Liga"), any())).thenAnswer(i -> i.getArgument(1));

            List<Team> result = playerService.scrapeTeamUrls("La Liga");

            assertThat(result).hasSize(1);
            Team team = result.get(0);
            assertThat(team.getName()).isEqualTo("Barcelona");
            assertThat(team.getUrl()).isEqualTo("https://whoscored.com/barcelona");
            assertThat(team.getLeague()).isEqualTo("La Liga");
        }

        @Test
        @DisplayName("persiste los equipos en el repositorio")
        void persistsTeamsToRepository() {
            when(whoScoredScraperService.scrapeTeamUrls("La Liga")).thenReturn(
                java.util.Map.of("Real Madrid", "https://whoscored.com/real-madrid")
            );
            when(teamRepository.saveTeamsForLeague(eq("La Liga"), any())).thenReturn(List.of());

            playerService.scrapeTeamUrls("La Liga");

            verify(teamRepository).saveTeamsForLeague(eq("La Liga"), any());
        }
    }

    @Nested
    @DisplayName("scrapeMatchStatsByMatchday")
    class ScrapeMatchStatsByMatchday {

        @Test
        @DisplayName("devuelve lista vacía cuando el scraper no encuentra partidos")
        void returnsEmptyListWhenNoMatchUrlsFound() {
            when(whoScoredScraperService.scrapeLeagueMatchUrls("Serie A")).thenReturn(List.of());

            List<PlayerMatchStats> result = playerService.scrapeMatchStatsByMatchday("Serie A");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("no toca el repositorio cuando no hay partidos que procesar")
        void doesNotInteractWithStatsRepositoryWhenNoMatchesFound() {
            when(whoScoredScraperService.scrapeLeagueMatchUrls("Serie A")).thenReturn(List.of());

            playerService.scrapeMatchStatsByMatchday("Serie A");

            verifyNoInteractions(matchStatsRepository);
        }
    }

    @Nested
    @DisplayName("scrapePlayersFromWhoScored")
    class ScrapePlayersFromWhoScored {

        @Test
        @DisplayName("lanza excepción cuando no hay equipos registrados para la liga")
        void throwsExceptionWhenNoTeamsRegisteredForLeague() {
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
