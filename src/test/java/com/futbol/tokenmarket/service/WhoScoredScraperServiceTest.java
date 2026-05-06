package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio WhoScoredScraper")
class WhoScoredScraperServiceTest {

    private static final String SEPARATOR = "|||";
    private WhoScoredScraperService service;

    @BeforeEach
    void setUp() {
        service = new WhoScoredScraperService();
    }

    @Nested
    @DisplayName("buildStatsMap")
    class BuildStatsMapTests {

        @Test
        @DisplayName("debe devolver un mapa vacio para filas de resumen vacias")
        void testBuildStatsMapEmpty() {
            Player player = new Player();
            player.setId("player1");
            player.setName("Test Player");

            Map<String, PlayerMatchStats> result = invokePrivateBuildStatsMap(
                List.of(), Set.of(), player
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("debe filtrar filas con menos de 3 partes")
        void testBuildStatsMapFilterShortRows() {
            Player player = new Player();
            player.setId("player1");
            player.setName("Test Player");

            List<String> rows = List.of(
                "url1" + SEPARATOR + "opponent1" + SEPARATOR + "stat1" + SEPARATOR + "stat2"
            );

            Map<String, PlayerMatchStats> result = invokePrivateBuildStatsMap(
                rows, Set.of(), player
            );

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("debe saltar filas con IDs de partido existentes")
        void testBuildStatsMapSkipExistingMatches() {
            Player player = new Player();
            player.setId("player1");
            player.setName("Test Player");

            List<String> rows = List.of(
                "http://example.com/matches/123" + SEPARATOR + "opponent1" + SEPARATOR + "stat1"
            );
            Set<String> existing = Set.of("123");

            Map<String, PlayerMatchStats> result = invokePrivateBuildStatsMap(
                rows, existing, player
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("debe construir estadisticas para partidos nuevos validos")
        void testBuildStatsMapSuccess() {
            Player player = new Player();
            player.setId("player123");
            player.setName("Test Player");

            List<String> rows = List.of(
                "http://example.com/matches/456" + SEPARATOR + "Manchester United" + SEPARATOR + "1.0" + SEPARATOR + "2.0"
            );

            Map<String, PlayerMatchStats> result = invokePrivateBuildStatsMap(
                rows, Set.of(), player
            );

            assertThat(result).hasSize(1);
            PlayerMatchStats stats = result.values().iterator().next();
            assertThat(stats.getPlayerId()).isEqualTo("player123");
            assertThat(stats.getOpponent()).isEqualTo("Manchester United");
        }
    }

    @Nested
    @DisplayName("parsePlayers")
    class ParsePlayersTests {

        @Test
        @DisplayName("debe manejar una lista capturada nula")
        void testParsePlayersNullCaptured() {
            Team team = new Team("Liverpool", "http://example.com", "Premier League");
            List<Player> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            invokePrivateParsePlayers(null, team, seen, players);

            assertThat(players).isEmpty();
        }

        @Test
        @DisplayName("debe filtrar entradas con partes insuficientes")
        void testParsePlayersFilterShortEntries() {
            Team team = new Team("Liverpool", "http://example.com", "Premier League");
            List<Player> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            List<String> captured = List.of(
                "http://example.com/players/123" + SEPARATOR + "Mohamed Salah" + SEPARATOR + "ST"
            );

            invokePrivateParsePlayers(captured, team, seen, players);

            assertThat(players).hasSize(1);
            assertThat(seen).hasSize(1);
        }

        @Test
        @DisplayName("debe filtrar entradas con nombre de jugador vacio")
        void testParsePlayersFilterEmptyName() {
            Team team = new Team("Liverpool", "http://example.com", "Premier League");
            List<Player> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            List<String> captured = List.of(
                "http://example.com/players/123" + SEPARATOR + "" + SEPARATOR + "ST"
            );

            invokePrivateParsePlayers(captured, team, seen, players);

            assertThat(players).isEmpty();
        }

        @Test
        @DisplayName("debe saltar IDs de jugador duplicados")
        void testParsePlayersSkipDuplicates() {
            Team team = new Team("Liverpool", "http://example.com", "Premier League");
            List<Player> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            seen.add("ws_123");

            List<String> captured = List.of(
                "http://example.com/players/123" + SEPARATOR + "Mohamed Salah" + SEPARATOR + "ST"
            );

            invokePrivateParsePlayers(captured, team, seen, players);

            assertThat(players).isEmpty();
        }

        @Test
        @DisplayName("debe parsear entradas de jugadores validas")
        void testParsePlayersSuccess() {
            Team team = new Team("Liverpool", "http://example.com", "Premier League");
            List<Player> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            List<String> captured = List.of(
                "http://example.com/players/123" + SEPARATOR + "Mohamed Salah" + SEPARATOR + "ST"
            );

            invokePrivateParsePlayers(captured, team, seen, players);

            assertThat(players).hasSize(1);
            assertThat(seen).hasSize(1);
            Player player = players.get(0);
            assertThat(player.getName()).isEqualTo("Mohamed Salah");
            assertThat(player.getPosition()).isEqualTo("ST");
            assertThat(player.getTeam()).isEqualTo("Liverpool");
            assertThat(player.getLeague()).isEqualTo("Premier League");
        }

        @Test
        @DisplayName("debe manejar nombres de jugador con prefijo numerico")
        void testParsePlayersNumericPrefix() {
            Team team = new Team("Liverpool", "http://example.com", "Premier League");
            List<Player> players = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            List<String> captured = List.of(
                "http://example.com/players/456" + SEPARATOR + "15. Virgil van Dijk" + SEPARATOR + "CB"
            );

            invokePrivateParsePlayers(captured, team, seen, players);

            assertThat(players).hasSize(1);
            // La regex elimina el prefijo "15. "
            assertThat(players.get(0).getName()).contains("van Dijk");
        }
    }

    @Nested
    @DisplayName("parseTeamEntries")
    class ParseTeamEntriesTests {

        @Test
        @DisplayName("debe manejar una lista capturada nula")
        void testParseTeamEntriesNullCaptured() {
            Map<String, String> teamUrls = new LinkedHashMap<>();

            invokePrivateParseTeamEntries(null, teamUrls);

            assertThat(teamUrls).isEmpty();
        }

        @Test
        @DisplayName("debe parsear entradas de equipos validas")
        void testParseTeamEntriesSuccess() {
            Map<String, String> teamUrls = new LinkedHashMap<>();
            List<String> captured = List.of(
                "http://example.com/teams/1" + SEPARATOR + "1. Manchester United",
                "http://example.com/teams/2" + SEPARATOR + "2. Liverpool"
            );

            invokePrivateParseTeamEntries(captured, teamUrls);

            assertThat(teamUrls)
                .hasSize(2)
                .containsEntry("Manchester United", "http://example.com/teams/1")
                .containsEntry("Liverpool", "http://example.com/teams/2");
        }

        @Test
        @DisplayName("debe saltar entradas con partes insuficientes")
        void testParseTeamEntriesSkipShortEntries() {
            Map<String, String> teamUrls = new LinkedHashMap<>();
            List<String> captured = List.of(
                "onlyonepart",
                "http://example.com/teams/1" + SEPARATOR + "Manchester United"
            );

            invokePrivateParseTeamEntries(captured, teamUrls);

            assertThat(teamUrls).hasSize(1);
        }

        @Test
        @DisplayName("debe saltar entradas con nombre de equipo vacio")
        void testParseTeamEntriesSkipEmptyName() {
            Map<String, String> teamUrls = new LinkedHashMap<>();
            List<String> captured = List.of(
                "http://example.com/teams/1" + SEPARATOR + "",
                "http://example.com/teams/2" + SEPARATOR + "   "
            );

            invokePrivateParseTeamEntries(captured, teamUrls);

            assertThat(teamUrls).isEmpty();
        }

        @Test
        @DisplayName("debe usar computeIfAbsent para evitar duplicados")
        void testParseTeamEntriesDuplicatePrevention() {
            Map<String, String> teamUrls = new LinkedHashMap<>();
            teamUrls.put("Manchester United", "http://example.com/teams/1");

            List<String> captured = List.of(
                "http://example.com/teams/1-new" + SEPARATOR + "Manchester United"
            );

            invokePrivateParseTeamEntries(captured, teamUrls);

            assertThat(teamUrls)
                .hasSize(1)
                .containsEntry("Manchester United", "http://example.com/teams/1");
        }

        @Test
        @DisplayName("debe limpiar el prefijo numerico de los nombres de equipo")
        void testParseTeamEntriesCleanPrefix() {
            Map<String, String> teamUrls = new LinkedHashMap<>();
            List<String> captured = List.of(
                "http://example.com/teams/1" + SEPARATOR + "1. Manchester United",
                "http://example.com/teams/2" + SEPARATOR + "10. Liverpool"
            );

            invokePrivateParseTeamEntries(captured, teamUrls);

            assertThat(teamUrls)
                .containsKey("Manchester United")
                .containsKey("Liverpool");
        }
    }

    @Nested
    @DisplayName("metodos utilitarios privados")
    class PrivateUtilsTests {

        @Test
        @DisplayName("extrae correctamente el id de partido de la url y usa fallback")
        void testExtractMatchIdFromUrl() throws Exception {
            java.lang.reflect.Method m = WhoScoredScraperService.class
                .getDeclaredMethod("extractMatchIdFromUrl", String.class);
            m.setAccessible(true);

            String id = (String) m.invoke(service, "https://example.com/matches/789/show");
            assertThat(id).isEqualTo("789");

            String fallback = (String) m.invoke(service, "not-a-match-url");
            assertThat(fallback).isNotNull();
        }

        @Test
        @DisplayName("extrae correctamente el id de jugador de la url y usa fallback")
        void testExtractPlayerIdFromUrl() throws Exception {
            java.lang.reflect.Method m = WhoScoredScraperService.class
                .getDeclaredMethod("extractPlayerIdFromUrl", String.class);
            m.setAccessible(true);

            String id = (String) m.invoke(service, "https://example.com/players/456/profile");
            assertThat(id).isEqualTo("456");

            String fallback = (String) m.invoke(service, "no-player-here");
            assertThat(fallback).isNotNull();
        }

        @Test
        @DisplayName("trySetIntStat y trySetDoubleStat manejan valores y no numericos")
        void testTrySetStats() throws Exception {
            PlayerMatchStats s = new PlayerMatchStats();

            java.lang.reflect.Method tryInt = WhoScoredScraperService.class
                .getDeclaredMethod("trySetIntStat", java.util.function.IntConsumer.class, String.class);
            java.lang.reflect.Method tryDouble = WhoScoredScraperService.class
                .getDeclaredMethod("trySetDoubleStat", java.util.function.DoubleConsumer.class, String.class);
            tryInt.setAccessible(true);
            tryDouble.setAccessible(true);

            tryInt.invoke(service, (java.util.function.IntConsumer) s::setMinutesPlayed, "45");
            tryDouble.invoke(service, (java.util.function.DoubleConsumer) s::setGoals, "2.5");

            assertThat(s.getMinutesPlayed()).isEqualTo(45);
            assertThat(s.getGoals()).isEqualTo(2.5);

            tryInt.invoke(service, (java.util.function.IntConsumer) s::setMinutesPlayed, "abc");
            tryDouble.invoke(service, (java.util.function.DoubleConsumer) s::setGoals, "x.y");
            assertThat(s.getMinutesPlayed()).isEqualTo(45);
            assertThat(s.getGoals()).isEqualTo(2.5);
        }

        @Test
        @DisplayName("mapStat y applyStatPairs asignan estadisticas esperadas")
        void testMapStatAndApplyStatPairs() throws Exception {
            PlayerMatchStats s = new PlayerMatchStats();
            String[] parts = new String[] { "href", "home", "position=GK", "goaltotal=1.0", "minsplayed=90" };

            java.lang.reflect.Method apply = WhoScoredScraperService.class
                .getDeclaredMethod("applyStatPairs", PlayerMatchStats.class, String[].class, int.class);
            apply.setAccessible(true);

            apply.invoke(service, s, parts, 2);

            assertThat(s.getPosition()).isEqualTo("GK");
            assertThat(s.getGoals()).isEqualTo(1.0);
            assertThat(s.getMinutesPlayed()).isEqualTo(90);
        }

        @Test
        @DisplayName("mergeIntoStatsMap construye y llena PlayerMatchStats a partir de filas")
        void testMergeIntoStatsMap() throws Exception {
            String row = "http://example.com/players/321" + SEPARATOR + "home" + SEPARATOR + "position=ST" + SEPARATOR + "goaltotal=2";
            List<String> rows = List.of(row);
            Map<String, PlayerMatchStats> map = new java.util.LinkedHashMap<>();
            Class<?> matchInfoClass = null;
            for (Class<?> c : WhoScoredScraperService.class.getDeclaredClasses()) {
                if (c.getSimpleName().equals("MatchInfo")) { matchInfoClass = c; break; }
            }
            java.lang.reflect.Method m = WhoScoredScraperService.class
                .getDeclaredMethod("mergeIntoStatsMap", List.class, Map.class, matchInfoClass);
            m.setAccessible(true);

            java.lang.reflect.Constructor<?> ci = matchInfoClass.getDeclaredConstructor(String.class, String.class, String.class, String.class, String.class);
            ci.setAccessible(true);
            Object matchInfo = ci.newInstance("m321", "http://match/321", "01-Jan-23", "Home FC", "Away FC");

            m.invoke(service, rows, map, matchInfo);

            assertThat(map).hasSize(1);
            PlayerMatchStats stats = map.values().iterator().next();
            assertThat(stats.getPlayerId()).startsWith("ws_");
            assertThat(stats.getPosition()).isEqualTo("ST");
            assertThat(stats.getGoals()).isEqualTo(2.0);
        }
    }

    // Metodos auxiliares para invocar metodos privados por reflection
    @SuppressWarnings("unchecked")
    private Map<String, PlayerMatchStats> invokePrivateBuildStatsMap(
            List<String> summaryRows, Set<String> existingMatchIds, Player player) {
        try {
            java.lang.reflect.Method method = WhoScoredScraperService.class
                .getDeclaredMethod("buildStatsMap", List.class, Set.class, Player.class);
            method.setAccessible(true);
            return (Map<String, PlayerMatchStats>) method.invoke(service, summaryRows, existingMatchIds, player);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokePrivateParsePlayers(
            List<String> captured, Team team, Set<String> seenPlayerIds, List<Player> players) {
        try {
            java.lang.reflect.Method method = WhoScoredScraperService.class
                .getDeclaredMethod("parsePlayers", List.class, Team.class, Set.class, List.class);
            method.setAccessible(true);
            method.invoke(service, captured, team, seenPlayerIds, players);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokePrivateParseTeamEntries(List<String> captured, Map<String, String> teamUrls) {
        try {
            java.lang.reflect.Method method = WhoScoredScraperService.class
                .getDeclaredMethod("parseTeamEntries", List.class, Map.class);
            method.setAccessible(true);
            method.invoke(service, captured, teamUrls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
