package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlayerMatchStatsRepository")
class PlayerMatchStatsRepositoryTest {

    @TempDir
    Path tempDir;

    private PlayerMatchStatsRepository repository;
    private File dataFile;

    @BeforeEach
    void setUp() {
        dataFile = tempDir.resolve("player_match_stats.json").toFile();
        repository = new PlayerMatchStatsRepository(new ObjectMapper(), dataFile.getAbsolutePath());
    }

    @Nested
    @DisplayName("getExistingMatchIds")
    class GetExistingMatchIds {

        @Test
        @DisplayName("devuelve vacío cuando no hay stats guardados")
        void returnsEmptyWhenNoStatsExist() throws IOException {
            Set<String> ids = repository.getExistingMatchIds();

            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("devuelve los matchIds de todos los registros")
        void returnsAllMatchIds() throws IOException {
            File temp = tempDir.resolve("temp.json").toFile();
            repository.appendToTemp(List.of(stat("id1", "match-A", "p1"), stat("id2", "match-B", "p2")), temp);
            repository.commitAndDeleteTemp(temp);

            Set<String> ids = repository.getExistingMatchIds();

            assertThat(ids).containsExactlyInAnyOrder("match-A", "match-B");
        }

        @Test
        @DisplayName("no repite matchIds aunque haya varios jugadores del mismo partido")
        void deduplicatesMatchIds() throws IOException {
            File temp = tempDir.resolve("temp.json").toFile();
            repository.appendToTemp(List.of(
                stat("id1", "match-A", "p1"),
                stat("id2", "match-A", "p2")
            ), temp);
            repository.commitAndDeleteTemp(temp);

            Set<String> ids = repository.getExistingMatchIds();

            assertThat(ids).containsExactly("match-A");
        }
    }

    @Nested
    @DisplayName("appendToTemp")
    class AppendToTemp {

        @Test
        @DisplayName("crea el archivo temporal si no existe")
        void createsFileIfNotExists() throws IOException {
            File temp = tempDir.resolve("temp.json").toFile();

            repository.appendToTemp(List.of(stat("id1", "match-A", "p1")), temp);

            assertThat(temp).exists();
        }

        @Test
        @DisplayName("acumula stats en llamadas sucesivas sin sobrescribir")
        void accumulatesStatsAcrossMultipleCalls() throws IOException {
            File temp = tempDir.resolve("temp.json").toFile();

            repository.appendToTemp(List.of(stat("id1", "match-A", "p1")), temp);
            repository.appendToTemp(List.of(stat("id2", "match-B", "p2")), temp);

            ObjectMapper mapper = new ObjectMapper();
            List<?> contents = mapper.readValue(temp, List.class);
            assertThat(contents).hasSize(2);
        }
    }

    @Nested
    @DisplayName("commitAndDeleteTemp")
    class CommitAndDeleteTemp {

        @Test
        @DisplayName("vuelca los stats del temporal al archivo principal")
        void mergesStatsFromTempIntoMain() throws IOException {
            File temp = tempDir.resolve("temp.json").toFile();
            repository.appendToTemp(List.of(stat("id1", "match-A", "p1")), temp);

            repository.commitAndDeleteTemp(temp);

            assertThat(repository.findAll()).hasSize(1);
            assertThat(repository.findAll().get(0).getId()).isEqualTo("id1");
        }

        @Test
        @DisplayName("elimina el archivo temporal después del commit")
        void deletesTempFileAfterCommit() throws IOException {
            File temp = tempDir.resolve("temp.json").toFile();
            repository.appendToTemp(List.of(stat("id1", "match-A", "p1")), temp);

            repository.commitAndDeleteTemp(temp);

            assertThat(temp).doesNotExist();
        }

        @Test
        @DisplayName("no agrega stats con IDs ya existentes en el archivo principal")
        void doesNotDuplicateStatsAlreadyInMain() throws IOException {
            File temp1 = tempDir.resolve("temp1.json").toFile();
            repository.appendToTemp(List.of(stat("id1", "match-A", "p1")), temp1);
            repository.commitAndDeleteTemp(temp1);

            File temp2 = tempDir.resolve("temp2.json").toFile();
            repository.appendToTemp(List.of(stat("id1", "match-A", "p1")), temp2);
            repository.commitAndDeleteTemp(temp2);

            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("no hace nada si el archivo temporal no existe")
        void doesNothingWhenTempFileDoesNotExist() throws IOException {
            File noExiste = tempDir.resolve("fantasma.json").toFile();

            repository.commitAndDeleteTemp(noExiste);

            assertThat(repository.findAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("createLeagueTempFile")
    class CreateLeagueTempFile {

        @Test
        @DisplayName("reemplaza espacios y caracteres especiales por guiones bajos")
        void sanitizesLeagueNameForFilename() {
            File temp = repository.createLeagueTempFile("Premier League");

            assertThat(temp).hasName("match_stats_Premier_League.tmp.json");
        }

        @Test
        @DisplayName("crea el archivo en el mismo directorio que el archivo principal")
        void createsFileInSameDirectoryAsMainFile() {
            File temp = repository.createLeagueTempFile("La Liga");

            assertThat(temp).hasParent(dataFile.getParentFile());
        }

        @Test
        @DisplayName("nombres de liga con caracteres no alfanuméricos quedan sanitizados")
        void sanitizesSpecialCharacters() {
            File temp = repository.createLeagueTempFile("Série A (Brasil)");

            assertThat(temp.getName()).doesNotContainAnyWhitespaces();
            assertThat(temp.getName()).doesNotContain("(", ")", "é");
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
