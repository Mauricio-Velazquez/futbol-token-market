package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TeamRepository")
class TeamRepositoryTest {

    @TempDir
    Path tempDir;

    private TeamRepository repository;

    @BeforeEach
    void setUp() {
        String path = tempDir.resolve("teams.json").toAbsolutePath().toString();
        repository = new TeamRepository(new ObjectMapper(), path);
    }

    @Nested
    @DisplayName("findByLeague")
    class FindByLeague {

        @Test
        @DisplayName("devuelve sólo los equipos de la liga indicada")
        void returnsOnlyTeamsFromThatLeague() throws IOException {
            repository.saveTeamsForLeague("La Liga", List.of(
                team("Barcelona", "La Liga"),
                team("Real Madrid", "La Liga")
            ));
            repository.saveTeamsForLeague("Premier League", List.of(
                team("Man City", "Premier League")
            ));

            List<Team> result = repository.findByLeague("La Liga");

            assertThat(result).extracting(Team::getName)
                .containsExactlyInAnyOrder("Barcelona", "Real Madrid");
        }

        @Test
        @DisplayName("la búsqueda por liga es case-insensitive")
        void leagueFilterIsCaseInsensitive() throws IOException {
            repository.saveTeamsForLeague("Ligue 1", List.of(team("PSG", "Ligue 1")));

            List<Team> result = repository.findByLeague("ligue 1");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("devuelve lista vacía cuando no hay equipos en esa liga")
        void returnsEmptyWhenNoTeamsInLeague() throws IOException {
            List<Team> result = repository.findByLeague("Bundesliga");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveTeamsForLeague")
    class SaveTeamsForLeague {

        @Test
        @DisplayName("reemplaza los equipos de la liga sin afectar las otras ligas")
        void replacesLeagueTeamsWithoutAffectingOthers() throws IOException {
            repository.saveTeamsForLeague("La Liga", List.of(team("Barça", "La Liga")));
            repository.saveTeamsForLeague("Premier League", List.of(team("Arsenal", "Premier League")));

            repository.saveTeamsForLeague("La Liga", List.of(team("Real Madrid", "La Liga")));

            assertThat(repository.findByLeague("La Liga"))
                .extracting(Team::getName).containsExactly("Real Madrid");
            assertThat(repository.findByLeague("Premier League"))
                .extracting(Team::getName).containsExactly("Arsenal");
        }

        @Test
        @DisplayName("devuelve los equipos que acaba de guardar")
        void returnsSavedTeams() throws IOException {
            List<Team> teams = List.of(team("Juventus", "Serie A"), team("Milan", "Serie A"));

            List<Team> result = repository.saveTeamsForLeague("Serie A", teams);

            assertThat(result).extracting(Team::getName)
                .containsExactlyInAnyOrder("Juventus", "Milan");
        }

        @Test
        @DisplayName("guardar lista vacía elimina todos los equipos de esa liga")
        void savingEmptyListRemovesAllTeamsOfLeague() throws IOException {
            repository.saveTeamsForLeague("Serie A", List.of(team("Inter", "Serie A")));

            repository.saveTeamsForLeague("Serie A", List.of());

            assertThat(repository.findByLeague("Serie A")).isEmpty();
        }
    }

    // --- helpers ---

    private Team team(String name, String league) {
        return new Team(name, "https://whoscored.com/" + name.toLowerCase(), league);
    }
}
