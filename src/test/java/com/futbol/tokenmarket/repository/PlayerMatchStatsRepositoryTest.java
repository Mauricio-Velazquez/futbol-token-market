package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.PlayerMatchStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PlayerMatchStatsRepository.class)
@Tag("e2e")
@DisplayName("PlayerMatchStatsRepository")
class PlayerMatchStatsRepositoryTest {

    @Autowired
    private PlayerMatchStatsRepository repository;

    @Nested
    @DisplayName("getExistingMatchIds")
    class GetExistingMatchIds {

        @Test
        @DisplayName("devuelve vacío cuando no hay stats guardados")
        void returnsEmptyWhenNoStatsExist() {
            Set<String> ids = repository.getExistingMatchIds();

            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("devuelve los matchIds de todos los registros")
        void returnsAllMatchIds() {
            repository.saveAllIfNew(List.of(stat("id1", "match-A", "p1"), stat("id2", "match-B", "p2")));

            Set<String> ids = repository.getExistingMatchIds();

            assertThat(ids).containsExactlyInAnyOrder("match-A", "match-B");
        }

        @Test
        @DisplayName("no repite matchIds aunque haya varios jugadores del mismo partido")
        void deduplicatesMatchIds() {
            repository.saveAllIfNew(List.of(
                stat("id1", "match-A", "p1"),
                stat("id2", "match-A", "p2")
            ));

            Set<String> ids = repository.getExistingMatchIds();

            assertThat(ids).containsExactly("match-A");
        }
    }

    @Nested
    @DisplayName("saveAllIfNew")
    class SaveAllIfNew {

        @Test
        @DisplayName("persiste todos los stats recibidos")
        void savesAllStats() {
            repository.saveAllIfNew(List.of(stat("id1", "match-A", "p1"), stat("id2", "match-B", "p2")));

            assertThat(repository.findAll()).hasSize(2);
        }

        @Test
        @DisplayName("no duplica stats con IDs ya existentes")
        void doesNotDuplicateExistingStats() {
            repository.saveAllIfNew(List.of(stat("id1", "match-A", "p1")));
            repository.saveAllIfNew(List.of(stat("id1", "match-A", "p1")));

            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("no hace nada con lista vacía")
        void doesNothingWithEmptyList() {
            repository.saveAllIfNew(List.of());

            assertThat(repository.findAll()).isEmpty();
        }
    }

    // --- helpers ---

    private PlayerMatchStats stat(String id, String matchId, String playerId) {
        PlayerMatchStats s = new PlayerMatchStats();
        s.setId(id);
        s.setMatchId(matchId);
        s.setPlayerId(playerId);
        return s;
    }
}
