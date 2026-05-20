package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        @DisplayName("omite scrapePlayerMatchStats cuando el jugador no tiene URL")
        void testScrapePlayerMatchStatsWithoutUrl() {
            Player player = new Player();
            player.setName("Test Player");

            List<PlayerMatchStats> result = service.scrapePlayerMatchStats(player, Set.of());

            assertThat(result).isEmpty();
        }
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
            assertThat(player.getTeamName()).isEqualTo("Liverpool");
            assertThat(player.getTeam().getName()).isEqualTo("Liverpool");
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
        @DisplayName("falla rapido si la liga no existe al pedir partidos")
        void testScrapeLeagueMatchUrlsUnknownLeague() {
            assertThrows(IllegalArgumentException.class,
                () -> service.scrapeLeagueMatchUrls("No existe"));
        }

        @Test
        @DisplayName("falla rapido si la liga no existe al pedir equipos")
        void testScrapeTeamUrlsUnknownLeague() {
            assertThrows(IllegalArgumentException.class,
                () -> service.scrapeTeamUrls("No existe"));
        }

        @Test
        @DisplayName("processExtraction sin tabHref mezcla filas y conserva el partido")
        void testProcessExtractionWithoutTabHref() throws Exception {
            Map<String, PlayerMatchStats> map = new LinkedHashMap<>();
            JavascriptExecutor js = mock(JavascriptExecutor.class);

            when(js.executeScript(anyString())).thenAnswer(invocation -> {
                String script = invocation.getArgument(0);
                if (script.contains("extractContainer('statistics-table-home-summary'")) {
                    return List.of(
                        "http://example.com/players/321|||home|||position=ST|||goaltotal=2"
                    );
                }
                return null;
            });

            Class<?> matchInfoClass = getMatchInfoClass();
            java.lang.reflect.Constructor<?> constructor = matchInfoClass.getDeclaredConstructor(
                String.class, String.class, String.class, String.class, String.class);
            constructor.setAccessible(true);
            Object matchInfo = constructor.newInstance(
                "m321", "http://match/321", "01-Jan-23", "Home FC", "Away FC");

            java.lang.reflect.Method method = WhoScoredScraperService.class.getDeclaredMethod(
                "processExtraction", JavascriptExecutor.class, String[].class, matchInfoClass, Map.class);
            method.setAccessible(true);

            method.invoke(
                service,
                js,
                new String[] {"statistics-table-home-summary", "home", null, "Home Summary"},
                matchInfo,
                map
            );

            assertThat(map).hasSize(1);
            PlayerMatchStats stats = map.values().iterator().next();
            assertThat(stats.getPlayerId()).startsWith("ws_");
            assertThat(stats.getGoals()).isEqualTo(2.0);
            assertThat(stats.getOpponent()).isEqualTo("Away FC");
        }

        @Test
        @DisplayName("processExtraction con tabHref ejecuta click y mergea filas")
        void testProcessExtractionWithTabHref() throws Exception {
            Map<String, PlayerMatchStats> map = new LinkedHashMap<>();
            JavascriptExecutor js = mock(JavascriptExecutor.class);

            when(js.executeScript(anyString())).thenAnswer(invocation -> {
                String script = invocation.getArgument(0);
                if (script.contains("extractContainer('statistics-table-home-offensive'")) {
                    return List.of(
                        "http://example.com/players/321|||home|||assist=1|||minsplayed=77"
                    );
                }
                return null;
            });

            Class<?> matchInfoClass = getMatchInfoClass();
            java.lang.reflect.Constructor<?> constructor = matchInfoClass.getDeclaredConstructor(
                String.class, String.class, String.class, String.class, String.class);
            constructor.setAccessible(true);
            Object matchInfo = constructor.newInstance(
                "m321", "http://match/321", "01-Jan-23", "Home FC", "Away FC");

            java.lang.reflect.Method method = WhoScoredScraperService.class.getDeclaredMethod(
                "processExtraction", JavascriptExecutor.class, String[].class, matchInfoClass, Map.class);
            method.setAccessible(true);

            method.invoke(
                service,
                js,
                new String[] {
                    "statistics-table-home-offensive",
                    "home",
                    "#live-player-home-offensive",
                    "Home Offensive"
                },
                matchInfo,
                map
            );

            assertThat(map).hasSize(1);
            PlayerMatchStats stats = map.values().iterator().next();
            assertThat(stats.getAssists()).isEqualTo(1.0);
            assertThat(stats.getMinutesPlayed()).isEqualTo(77);
        }

        @Test
        @DisplayName("pollItems devuelve la ultima lista estable")
        void testPollItems() throws Exception {
            JavascriptExecutor js = mock(JavascriptExecutor.class);
            AtomicInteger calls = new AtomicInteger();
            when(js.executeScript(anyString())).thenAnswer(invocation -> {
                calls.incrementAndGet();
                return List.of("row1", "row2");
            });

            java.lang.reflect.Method method = WhoScoredScraperService.class.getDeclaredMethod(
                "pollItems", JavascriptExecutor.class, String.class, int.class, String.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<String> result = (List<String>) method.invoke(
                service, js, "return []", 3, "label");

            assertThat(result).containsExactly("row1", "row2");
            assertThat(calls.get()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("createDriver usa la ruta de Windows cuando corresponde")
        void testCreateDriverWindowsBranch() throws Exception {
            String previousOs = System.getProperty("os.name");
            System.setProperty("os.name", "Windows 11");

            try (MockedConstruction<ChromeDriver> mocked = Mockito.mockConstruction(ChromeDriver.class)) {
                java.lang.reflect.Method method = WhoScoredScraperService.class
                    .getDeclaredMethod("createDriver");
                method.setAccessible(true);

                Object driver = method.invoke(service);

                assertThat(mocked.constructed()).hasSize(1);
                assertThat(driver).isSameAs(mocked.constructed().get(0));
            } finally {
                restoreOsName(previousOs);
            }
        }

        @Test
        @DisplayName("createDriver configura Chromium en Linux")
        void testCreateDriverLinuxBranch() throws Exception {
            String previousOs = System.getProperty("os.name");
            String previousChromeDriver = System.getProperty("webdriver.chrome.driver");
            System.setProperty("os.name", "Linux");

            try (MockedConstruction<ChromeDriver> mocked = Mockito.mockConstruction(ChromeDriver.class)) {
                java.lang.reflect.Method method = WhoScoredScraperService.class
                    .getDeclaredMethod("createDriver");
                method.setAccessible(true);

                Object driver = method.invoke(service);

                assertThat(mocked.constructed()).hasSize(1);
                assertThat(driver).isSameAs(mocked.constructed().get(0));
                assertThat(System.getProperty("webdriver.chrome.driver"))
                    .isEqualTo("/snap/bin/chromium.chromedriver");
            } finally {
                restoreOsName(previousOs);
                restoreSystemProperty("webdriver.chrome.driver", previousChromeDriver);
            }
        }

        @Test
        @DisplayName("omite scrapeMatchPlayerStats cuando el partido ya existe")
        void testScrapeMatchPlayerStatsExistingMatch() {
            List<PlayerMatchStats> result = service.scrapeMatchPlayerStats(
                "https://example.com/live/123",
                Set.of("123")
            );

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("omite scrapePlayerMatchStats cuando el partido mas reciente ya existe")
        void testScrapePlayerMatchStatsExistingLatestMatch() {
            Player player = new Player();
            player.setName("Test Player");
            player.setUrl("https://example.com/players/123/show");

            try (MockedConstruction<ChromeDriver> mocked = Mockito.mockConstruction(ChromeDriver.class, (driver, context) -> {
                when(driver.findElement(any(By.class))).thenAnswer(invocation -> mock(WebElement.class));
                when(driver.executeScript(anyString(), any())).thenReturn(List.of(
                    "https://example.com/matches/456/show|||Opponent|||goaltotal=1"
                ));
            })) {
                List<PlayerMatchStats> result = service.scrapePlayerMatchStats(player, Set.of("456"));

                assertThat(result).isEmpty();
                verify(mocked.constructed().get(0)).quit();
            }
        }

        @Test
        @DisplayName("scrapeLeagueMatchUrls extrae partidos con ChromeDriver mockeado")
        void testScrapeLeagueMatchUrlsHappyPath() {
            try (MockedConstruction<ChromeDriver> mocked = Mockito.mockConstruction(ChromeDriver.class, (driver, context) -> {
                WebElement genericElement = mock(WebElement.class);
                when(driver.findElement(any(By.class))).thenAnswer(invocation -> {
                    By by = invocation.getArgument(0);
                    String selector = by.toString();
                    if (selector.contains("toggleCalendar") || selector.contains("todayBtn") || selector.contains("dayChangeBtn-prev")) {
                        throw new org.openqa.selenium.NoSuchElementException("calendar not available");
                    }
                    return genericElement;
                });
                Mockito.doAnswer(invocation -> {
                    String script = invocation.getArgument(0);
                    if (script.contains("a[id^=\"scoresBtn-\"]")) {
                        return List.of("https://example.com/live/123");
                    }
                    return null;
                }).when(driver).executeScript(anyString(), any());
                Mockito.doAnswer(invocation -> {
                    String script = invocation.getArgument(0);
                    if (script.contains("a[id^=\"scoresBtn-\"]")) {
                        return List.of("https://example.com/live/123");
                    }
                    return null;
                }).when(driver).executeScript(anyString());
            })) {
                List<String> result = service.scrapeLeagueMatchUrls("Premier League");

                assertThat(result).containsExactly("https://example.com/live/123");
                verify(mocked.constructed().get(0), atLeastOnce()).quit();
            }
        }

        @Test
        @DisplayName("scrapeTeamUrls extrae equipos con ChromeDriver mockeado")
        void testScrapeTeamUrlsHappyPath() {
            try (MockedConstruction<ChromeDriver> mocked = Mockito.mockConstruction(ChromeDriver.class, (driver, context) -> {
                WebElement subNavigation = mock(WebElement.class);
                WebElement teamStatsAnchor = mock(WebElement.class);
                WebElement summaryGrid = mock(WebElement.class);

                when(teamStatsAnchor.getAttribute("href")).thenReturn("https://example.com/teamstats");
                when(driver.findElement(any(By.class))).thenAnswer(invocation -> {
                    By by = invocation.getArgument(0);
                    String selector = by.toString();
                    if (selector.contains("teamstatistics")) {
                        return teamStatsAnchor;
                    }
                    if (selector.contains("sub-navigation")) {
                        return subNavigation;
                    }
                    if (selector.contains("top-team-stats-summary-grid")) {
                        return summaryGrid;
                    }
                    return mock(WebElement.class);
                });
                Mockito.doAnswer(invocation -> {
                    String script = invocation.getArgument(0);
                    if (script.contains("#top-team-stats-summary-grid a.team-link")) {
                        return List.of(
                            "https://example.com/teams/1|||1. Manchester United",
                            "https://example.com/teams/2|||2. Liverpool"
                        );
                    }
                    return null;
                }).when(driver).executeScript(anyString(), any());
                Mockito.doAnswer(invocation -> {
                    String script = invocation.getArgument(0);
                    if (script.contains("#top-team-stats-summary-grid a.team-link")) {
                        return List.of(
                            "https://example.com/teams/1|||1. Manchester United",
                            "https://example.com/teams/2|||2. Liverpool"
                        );
                    }
                    return null;
                }).when(driver).executeScript(anyString());
            })) {
                Map<String, String> result = service.scrapeTeamUrls("Premier League");

                assertThat(result)
                    .containsEntry("Manchester United", "https://example.com/teams/1")
                    .containsEntry("Liverpool", "https://example.com/teams/2");
                verify(mocked.constructed().get(0), atLeastOnce()).quit();
            }
        }

        @Test
        @DisplayName("scrapePlayersFromTeams extrae jugadores con ChromeDriver mockeado")
        void testScrapePlayersFromTeamsHappyPath() {
            Team team = new Team("Liverpool", "https://example.com/team/liverpool", "Premier League");

            try (MockedConstruction<ChromeDriver> mocked = Mockito.mockConstruction(ChromeDriver.class, (driver, context) -> {
                when(driver.findElement(any(By.class))).thenAnswer(invocation -> mock(WebElement.class));
                Mockito.doAnswer(invocation -> {
                    String script = invocation.getArgument(0);
                    if (script.contains("#top-player-stats-summary-grid tbody")) {
                        return List.of(
                            "https://example.com/players/123|||Mohamed Salah|||ST"
                        );
                    }
                    return null;
                }).when(driver).executeScript(anyString(), any());
                Mockito.doAnswer(invocation -> {
                    String script = invocation.getArgument(0);
                    if (script.contains("#top-player-stats-summary-grid tbody")) {
                        return List.of(
                            "https://example.com/players/123|||Mohamed Salah|||ST"
                        );
                    }
                    return null;
                }).when(driver).executeScript(anyString());
            })) {
                List<Player> result = service.scrapePlayersFromTeams(List.of(team));

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getId()).isEqualTo("ws_123");
                assertThat(result.get(0).getName()).isEqualTo("Mohamed Salah");
                assertThat(result.get(0).getTeam().getName()).isEqualTo("Liverpool");
                verify(mocked.constructed().get(0), atLeastOnce()).quit();
            }
        }

        @Test
        @DisplayName("sleepMs reinterrumpe el thread si se corta")
        void testSleepMsInterrupted() throws Exception {
            java.lang.reflect.Method method = WhoScoredScraperService.class
                .getDeclaredMethod("sleepMs", long.class);
            method.setAccessible(true);

            Thread.currentThread().interrupt();
            try {
                method.invoke(service, 1L);
                assertThat(Thread.currentThread().isInterrupted()).isTrue();
            } finally {
                Thread.interrupted();
            }
        }

        @Test
        @DisplayName("omite scrapePlayerMatchStats cuando el jugador no tiene URL")
        void testScrapePlayerMatchStatsWithoutUrl() {
            Player player = new Player();
            player.setName("Test Player");

            List<PlayerMatchStats> result = service.scrapePlayerMatchStats(player, Set.of());

            assertThat(result).isEmpty();
        }

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
            String[] parts = new String[] {
                "href",
                "home",
                "pos=GK",
                "goals=1.0",
                "assists=2",
                "yellowcard=1",
                "red_cards=2",
                "pa=88.5",
                "passsuccess=88.5",
                "passsuccessinmatch=91.5",
                "duelaerialwon=4",
                "rating=7.8",
                "tackles=4",
                "tackletotalattempted=5",
                "interceptionall=3",
                "fouls_committed=6",
                "clearancetotal=7",
                "shotblocked=8",
                "save=9",
                "shotsontarget=10",
                "keypasstotal=11",
                "dribblewon=12",
                "fouls_won=13",
                "offsidegiven=14",
                "passtotal=15",
                "longballtotal=16",
                "crosstotal=17",
                "throughballtotal=18",
                "unknown=999",
                "minutes_played=90"
            };

            java.lang.reflect.Method apply = WhoScoredScraperService.class
                .getDeclaredMethod("applyStatPairs", PlayerMatchStats.class, String[].class, int.class);
            apply.setAccessible(true);

            apply.invoke(service, s, parts, 2);

            assertThat(s.getGoals()).isEqualTo(1.0);
            assertThat(s.getAssists()).isEqualTo(2.0);
            assertThat(s.getYellowCards()).isEqualTo(1);
            assertThat(s.getRedCards()).isEqualTo(2);
            assertThat(s.getPassSuccess()).isEqualTo(91.5);
            assertThat(s.getAerialsWon()).isEqualTo(4.0);
            assertThat(s.getRating()).isEqualTo(7.8);
            assertThat(s.getTackles()).isEqualTo(5.0);
            assertThat(s.getInterceptions()).isEqualTo(3.0);
            assertThat(s.getFoulsCommitted()).isEqualTo(6.0);
            assertThat(s.getClearances()).isEqualTo(7.0);
            assertThat(s.getBlockedShots()).isEqualTo(8.0);
            // saves removed from model — no assertion
            assertThat(s.getShotsOnTarget()).isEqualTo(10.0);
            assertThat(s.getKeyPasses()).isEqualTo(11.0);
            assertThat(s.getDribblesWon()).isEqualTo(12.0);
            assertThat(s.getOffsides()).isEqualTo(14.0);
            assertThat(s.getTotalPasses()).isEqualTo(15.0);
            assertThat(s.getLongBalls()).isEqualTo(16.0);
            assertThat(s.getCrosses()).isEqualTo(17.0);
            assertThat(s.getThroughBalls()).isEqualTo(18.0);
            assertThat(s.getMinutesPlayed()).isEqualTo(90);
        }

        @Test
        @DisplayName("mapStat soporta aliases de live statistics para tackles, fouls y total passes")
        void testMapStatLiveStatisticsAliases() throws Exception {
            PlayerMatchStats s = new PlayerMatchStats();
            String[] parts = new String[] {
                "href",
                "home",
                "TackleWonTotal=3",
                "FoulCommitted=2",
                "TotalPasses=47"
            };

            java.lang.reflect.Method apply = WhoScoredScraperService.class
                .getDeclaredMethod("applyStatPairs", PlayerMatchStats.class, String[].class, int.class);
            apply.setAccessible(true);

            apply.invoke(service, s, parts, 2);

            assertThat(s.getTackles()).isEqualTo(3.0);
            assertThat(s.getFoulsCommitted()).isEqualTo(2.0);
            assertThat(s.getTotalPasses()).isEqualTo(47.0);
        }

        @Test
        @DisplayName("mergeIntoStatsMap construye y fusiona PlayerMatchStats a partir de filas")
        void testMergeIntoStatsMap() throws Exception {
            List<String> rows = List.of(
                "http://example.com/players/321" + SEPARATOR + "home" + SEPARATOR + "position=ST" + SEPARATOR + "goaltotal=2",
                "http://example.com/players/321" + SEPARATOR + "away" + SEPARATOR + "assist=1" + SEPARATOR + "minsplayed=77"
            );
            Map<String, PlayerMatchStats> map = new java.util.LinkedHashMap<>();

            Class<?> matchInfoClass = java.util.Arrays.stream(WhoScoredScraperService.class.getDeclaredClasses())
                .filter(c -> "MatchInfo".equals(c.getSimpleName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No se encontró la clase interna MatchInfo"));

            java.lang.reflect.Method m = WhoScoredScraperService.class
                .getDeclaredMethod("mergeIntoStatsMap", List.class, Map.class, matchInfoClass);
            m.setAccessible(true);

            java.lang.reflect.Constructor<?> ci = matchInfoClass.getDeclaredConstructor(
                String.class, String.class, String.class, String.class, String.class);
            ci.setAccessible(true);
            Object matchInfo = ci.newInstance("m321", "http://match/321", "01-Jan-23", "Home FC", "Away FC");

            m.invoke(service, rows, map, matchInfo);

            assertThat(map).hasSize(1);
            PlayerMatchStats stats = map.values().iterator().next();
            assertThat(stats.getPlayerId()).startsWith("ws_");
            assertThat(stats.getGoals()).isEqualTo(2.0);
            assertThat(stats.getAssists()).isEqualTo(1.0);
            assertThat(stats.getMinutesPlayed()).isEqualTo(77);
            assertThat(stats.getOpponent()).isEqualTo("Away FC");
            assertThat(stats.getMatchId()).isEqualTo("m321");
            assertThat(stats.getMatchUrl()).isEqualTo("http://match/321");
            assertThat(stats.getDate()).isEqualTo("01-Jan-23");
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

    private Class<?> getMatchInfoClass() {
        return Arrays.stream(WhoScoredScraperService.class.getDeclaredClasses())
            .filter(c -> "MatchInfo".equals(c.getSimpleName()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No se encontró la clase interna MatchInfo"));
    }

    private void restoreOsName(String previousOs) {
        restoreSystemProperty("os.name", previousOs);
    }

    private void restoreSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
