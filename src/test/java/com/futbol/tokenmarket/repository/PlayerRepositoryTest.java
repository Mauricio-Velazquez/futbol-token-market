package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PlayerRepository.class)
@DisplayName("PlayerRepository")
class PlayerRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private PlayerRepository repository;

    @Nested
    @DisplayName("findByLeague")
    class FindByLeague {

        @Test
        @DisplayName("devuelve sólo los jugadores de la liga indicada")
        void returnsOnlyPlayersFromThatLeague() {
            repository.save(player("p1", "Messi", "La Liga", "Inter Miami", "FW"));
            repository.save(player("p2", "Haaland", "Premier League", "Man City", "FW"));
            repository.save(player("p3", "Pedri", "La Liga", "Barcelona", "M(C)"));

            List<Player> result = repository.findByLeague("La Liga");

            assertThat(result).extracting(Player::getName)
                .containsExactlyInAnyOrder("Messi", "Pedri");
        }

        @Test
        @DisplayName("la búsqueda por liga es case-insensitive")
        void leagueFilterIsCaseInsensitive() {
            repository.save(player("p1", "Mbappé", "ligue 1", "PSG", "FW"));

            List<Player> result = repository.findByLeague("Ligue 1");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("devuelve lista vacía cuando ningún jugador pertenece a esa liga")
        void returnsEmptyWhenNoPlayerBelongsToLeague() {
            repository.save(player("p1", "Messi", "La Liga", "Inter Miami", "FW"));

            List<Player> result = repository.findByLeague("Bundesliga");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByFilters — posición")
    class FindByFiltersPosition {

        @Test
        @DisplayName("filtra porteros con posición 'GK'")
        void filtersGoalkeepers() {
            repository.save(player("p1", "Ter Stegen", "La Liga", "Barcelona", "GK"));
            repository.save(player("p2", "Pedri", "La Liga", "Barcelona", "M(C)"));

            List<Player> result = repository.findByFilters(null, null, "GK");

            assertThat(result).extracting(Player::getName).containsExactly("Ter Stegen");
        }

        @Test
        @DisplayName("filtra defensas con patrón 'D'")
        void filtersDefenders() {
            repository.save(player("p1", "Carvajal", "La Liga", "Real Madrid", "D(R)"));
            repository.save(player("p2", "Vinicius", "La Liga", "Real Madrid", "FW,AM(L)"));

            List<Player> result = repository.findByFilters(null, null, "D");

            assertThat(result).extracting(Player::getName).containsExactly("Carvajal");
        }

        @Test
        @DisplayName("filtra mediocampistas defensivos con patrón 'DM'")
        void filtersDefensiveMidfielders() {
            repository.save(player("p1", "Casemiro", "Premier League", "Man United", "DML"));
            repository.save(player("p2", "Bellingham", "La Liga", "Real Madrid", "M(C),AM(C)"));

            List<Player> result = repository.findByFilters(null, null, "DM");

            assertThat(result).extracting(Player::getName).containsExactly("Casemiro");
        }

        @Test
        @DisplayName("filtra mediocampistas con patrón 'M'")
        void filtersMidfielders() {
            repository.save(player("p1", "Pedri", "La Liga", "Barcelona", "M(C),AM(C)"));
            repository.save(player("p2", "Yamal", "La Liga", "Barcelona", "AM(R),FW"));

            List<Player> result = repository.findByFilters(null, null, "M");

            assertThat(result).extracting(Player::getName).containsExactly("Pedri");
        }

        @Test
        @DisplayName("filtra mediapuntas con patrón 'AM'")
        void filtersAttackingMidfielders() {
            repository.save(player("p1", "Dybala", "Serie A", "Roma", "AM(C)"));
            repository.save(player("p2", "Lukaku", "Serie A", "Roma", "FW"));

            List<Player> result = repository.findByFilters(null, null, "AM");

            assertThat(result).extracting(Player::getName).containsExactly("Dybala");
        }

        @Test
        @DisplayName("filtra delanteros con patrón 'FW'")
        void filtersForwards() {
            repository.save(player("p1", "Giroud", "Serie A", "Milan", "FW"));
            repository.save(player("p2", "Hernandez", "Serie A", "Milan", "D(L)"));

            List<Player> result = repository.findByFilters(null, null, "FW");

            assertThat(result).extracting(Player::getName).containsExactly("Giroud");
        }

        @Test
        @DisplayName("un jugador multiposición aparece en el filtro de cada posición que ocupa")
        void multiPositionPlayerMatchesEachOfTheirPositions() {
            repository.save(player("p1", "Bellingham", "La Liga", "Real Madrid", "M(C),AM(C)"));

            assertThat(repository.findByFilters(null, null, "M"))
                .extracting(Player::getName).containsExactly("Bellingham");
            assertThat(repository.findByFilters(null, null, "AM"))
                .extracting(Player::getName).containsExactly("Bellingham");
        }

        @Test
        @DisplayName("ignora el filtro de posición cuando no es un código reconocido")
        void ignoresUnknownPositionCode() {
            repository.save(player("p1", "Messi", "La Liga", "Inter Miami", "FW"));

            List<Player> result = repository.findByFilters(null, null, "UNKNOWN");

            assertThat(result).extracting(Player::getName).containsExactly("Messi");
        }
    }

    @Nested
    @DisplayName("findByFilters — combinaciones")
    class FindByFiltersCombinations {

        @Test
        @DisplayName("combina liga y equipo correctamente")
        void combinesLeagueAndTeamFilters() {
            repository.save(player("p1", "Pedri", "La Liga", "Barcelona", "M(C)"));
            repository.save(player("p2", "Bellingham", "La Liga", "Real Madrid", "M(C)"));
            repository.save(player("p3", "Haaland", "Premier League", "Man City", "FW"));

            List<Player> result = repository.findByFilters("La Liga", "Barcelona", null);

            assertThat(result).extracting(Player::getName).containsExactly("Pedri");
        }

        @Test
        @DisplayName("combina liga, equipo y posición")
        void combinesAllThreeFilters() {
            repository.save(player("p1", "Pedri", "La Liga", "Barcelona", "M(C)"));
            repository.save(player("p2", "Yamal", "La Liga", "Barcelona", "AM(R),FW"));
            repository.save(player("p3", "Ter Stegen", "La Liga", "Barcelona", "GK"));

            List<Player> result = repository.findByFilters("La Liga", "Barcelona", "GK");

            assertThat(result).extracting(Player::getName).containsExactly("Ter Stegen");
        }

        @Test
        @DisplayName("devuelve todos cuando todos los filtros son nulos")
        void returnsAllPlayersWhenNoFiltersApplied() {
            repository.save(player("p1", "Messi", "La Liga", "Inter Miami", "FW"));
            repository.save(player("p2", "Haaland", "Premier League", "Man City", "FW"));

            List<Player> result = repository.findByFilters(null, null, null);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("save y delete")
    class SaveAndDelete {

        @Test
        @DisplayName("guarda un jugador nuevo y lo recupera por ID")
        void savesPlayerAndRetrievesById() {
            Player p = player("p1", "Messi", "La Liga", "Inter Miami", "FW");

            repository.save(p);

            Optional<Player> found = repository.findById("p1");
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Messi");
            assertThat(found.get().getTeamName()).isEqualTo("Inter Miami");
        }

        @Test
        @DisplayName("actualizar un jugador existente reemplaza sus datos")
        void savingExistingPlayerReplacesIt() {
            repository.save(player("p1", "Messi", "La Liga", "Inter Miami", "FW"));
            Player updated = player("p1", "Lionel Messi", "MLS", "Inter Miami", "FW");

            repository.save(updated);

            assertThat(repository.findById("p1").get().getName()).isEqualTo("Lionel Messi");
            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("eliminar un jugador devuelve true y lo remueve de la lista")
        void deletingPlayerReturnsTrueAndRemovesIt() {
            repository.save(player("p1", "Messi", "La Liga", "Inter Miami", "FW"));

            boolean result = repository.deleteById("p1");

            assertThat(result).isTrue();
            assertThat(repository.findAll()).isEmpty();
        }

        @Test
        @DisplayName("eliminar un ID inexistente devuelve false")
        void deletingNonExistentPlayerReturnsFalse() {
            boolean result = repository.deleteById("fantasma");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("savePlayersForLeague")
    class SavePlayersForLeague {

        @Test
        @DisplayName("agrega o actualiza jugadores sin borrar los existentes")
        void upsertsPlayersWithoutDeletingExistingOnes() {
            repository.save(player("p1", "Haaland", "Premier League", "Man City", "FW"));
            repository.save(player("p2", "Messi", "La Liga", "Inter Miami", "FW"));

            List<Player> newLaLiga = List.of(
                player("p2", "Messi", "La Liga", "Inter Miami", "FW"),
                player("p3", "Pedri", "La Liga", "Barcelona", "M(C)")
            );
            repository.savePlayersForLeague("La Liga", newLaLiga);

            assertThat(repository.findByLeague("La Liga"))
                .extracting(Player::getName).containsExactlyInAnyOrder("Messi", "Pedri");
            assertThat(repository.findByLeague("Premier League"))
                .extracting(Player::getName).containsExactly("Haaland");
            assertThat(repository.findById("p3")).get().extracting(Player::getTeamName)
                .isEqualTo("Barcelona");
        }
    }

    // --- helpers ---

    private Player player(String id, String name, String league, String teamName, String position) {
        Team team = testEntityManager.persistAndFlush(new Team(teamName, "", league));
        Player p = new Player();
        p.setId(id);
        p.setName(name);
        p.setLeague(league);
        p.setTeamName(teamName);
        p.setTeam(team);
        p.setPosition(position);
        return p;
    }
}
