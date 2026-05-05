package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WhoScoredScraperService {

    private static final String WHOSCORED_BASE = "https://www.whoscored.com";

    private static final Map<String, String> LEAGUE_URLS = Map.of(
        "Premier League", WHOSCORED_BASE + "/regions/252/tournaments/2/england-premier-league",
        "La Liga",        WHOSCORED_BASE + "/regions/206/tournaments/4/spain-laliga",
        "Serie A",        WHOSCORED_BASE + "/regions/108/tournaments/5/italy-serie-a",
        "Bundesliga",     WHOSCORED_BASE + "/regions/81/tournaments/3/germany-bundesliga",
        "Ligue 1",        WHOSCORED_BASE + "/regions/74/tournaments/22/france-ligue-1"
    );

    /**
     * Navigates to the league summary page and returns URLs of COMPLETED matches
     * (status=6/FT) from the currently displayed matchday window.
     *
     * Strategy: read the hypernova JSON of the fixtures widget (authoritative list of
     * matches + status for the displayed week), filter to status=6, then look up each
     * match's full /live/ URL in the DOM.
     */
    public List<String> scrapeLeagueMatchUrls(String leagueName) {
        String leagueUrl = LEAGUE_URLS.get(leagueName);
        if (leagueUrl == null) {
            throw new IllegalArgumentException("Liga no soportada: " + leagueName);
        }

        WebDriver driver = createDriver();
        List<String> matchUrls = new ArrayList<>();
        try {
            driver.get(leagueUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("script[data-hypernova-key='tournamentfixtures']")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. Parse the hypernova JSON to get IDs of finished matches (status=6)
            //    and look up each one's /live/ URL in the DOM.
            String extractScript =
                "try {" +
                "  var el = document.querySelector('script[type=\"application/json\"][data-hypernova-key=\"tournamentfixtures\"]');" +
                "  if (!el) return [];" +
                "  var json = JSON.parse(el.textContent.trim().replace(/^<!--/, '').replace(/-->$/, ''));" +
                "  var urls = [];" +
                "  (json.tournaments || []).forEach(function(t) {" +
                "    (t.matches || []).forEach(function(m) {" +
                "      if (m.status !== 6) return;" + // 6 = FT
                "      var link = document.querySelector('a[href*=\"/matches/' + m.id + '/live/\"]');" +
                "      if (link) urls.push(link.href);" +
                "    });" +
                "  });" +
                "  return urls;" +
                "} catch(e) { return []; }";

            int prevCount = -1;
            for (int i = 0; i < 15; i++) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) js.executeScript(extractScript);
                int count = items != null ? items.size() : 0;
                System.out.println("[WhoScored] " + leagueName + " - partidos FT encontrados: " + count);
                if (items != null && count > 0) matchUrls = new ArrayList<>(items);
                if (count > 0 && count == prevCount) break;
                prevCount = count;
            }

            System.out.println("[WhoScored] " + leagueName + " - total partidos FT: " + matchUrls.size());
        } finally {
            driver.quit();
        }
        return matchUrls;
    }

    public Map<String, String> scrapeTeamUrls(String leagueName) {
        String leagueUrl = LEAGUE_URLS.get(leagueName);
        if (leagueUrl == null) {
            throw new IllegalArgumentException("Liga no soportada para WhoScored: " + leagueName);
        }

        WebDriver driver = createDriver();
        Map<String, String> teamUrls = new LinkedHashMap<>();

        try {
            // Paso 1: cargar la página de la liga para obtener la URL de Team Statistics
            driver.get(leagueUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sub-navigation")));
            WebElement teamStatsAnchor = driver.findElement(
                By.cssSelector("#sub-navigation a[href*='teamstatistics']")
            );
            String teamStatsUrl = teamStatsAnchor.getAttribute("href");
            System.out.println("[WhoScored] Navegando a: " + teamStatsUrl);

            // Paso 2: navegar directo a la página de estadísticas de equipos
            driver.get(teamStatsUrl);

            // Paso 3: esperar que la tabla aparezca en el DOM
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("top-team-stats-summary-grid")
            ));

            // JS modifica el tbody continuamente; usamos JavascriptExecutor para capturar
            // href + texto en un único paso atómico, evitando que el DOM cambie entre llamadas.
            // Polleamos hasta que el conteo se estabilice en 2 checks consecutivos.
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String extractScript =
                "return Array.from(document.querySelectorAll(" +
                "  '#top-team-stats-summary-grid a.team-link'" +
                ")).map(a => a.href + '|||' + a.textContent.trim());";

            int prevCount = -1;
            List<String> captured = List.of();
            for (int i = 0; i < 20; i++) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) js.executeScript(extractScript);
                int count = items != null ? items.size() : 0;
                System.out.println("[WhoScored] Filas cargadas: " + count);
                if (items != null && count > 0) captured = items;
                if (count > 0 && count == prevCount) break;
                prevCount = count;
            }

            // Paso 4: parsear los datos capturados
            for (String entry : captured) {
                String[] parts = entry.split("\\|\\|\\|", 2);
                if (parts.length == 2) {
                    String href = parts[0].trim();
                    String name = parts[1].trim().replaceAll("^\\d+\\.\\s*", "");
                    if (!name.isEmpty() && !teamUrls.containsKey(name)) {
                        teamUrls.put(name, href);
                    }
                }
            }
            System.out.println("[WhoScored] Equipos encontrados: " + teamUrls.size());
        } finally {
            driver.quit();
        }

        return teamUrls;
    }

    public List<Player> scrapePlayersFromTeams(List<Team> teams) {
        List<Player> allPlayers = new ArrayList<>();
        WebDriver driver = createDriver();
        try {
            for (Team team : teams) {
                System.out.println("[WhoScored] Scrapeando jugadores de: " + team.getName());
                List<Player> teamPlayers = scrapePlayersFromTeam(driver, team);
                System.out.println("[WhoScored] Jugadores encontrados en " + team.getName() + ": " + teamPlayers.size());
                allPlayers.addAll(teamPlayers);
            }
        } finally {
            driver.quit();
        }
        return allPlayers;
    }

    private List<Player> scrapePlayersFromTeam(WebDriver driver, Team team) {
        List<Player> players = new ArrayList<>();
        Set<String> seenPlayerIds = new HashSet<>();
        try {
            driver.get(team.getUrl());
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.player-link")));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            String extractScript =
                "var table = document.querySelector('#top-player-stats-summary-grid tbody');" +
                "if (!table) return [];" +
                "return Array.from(table.querySelectorAll('tr')).map(function(row) {" +
                "  var link = row.querySelector('a.player-link');" +
                "  if (!link) return null;" +
                "  var cells = row.querySelectorAll('td');" +
                "  var posMeta = cells[0] ? cells[0].querySelector('.player-meta-data:last-child') : null;" +
                "  var pos = posMeta ? posMeta.textContent.trim().replace(/^,\\s*/, '') : 'Unknown';" +
                "  return link.href + '|||' + link.textContent.trim() + '|||' + pos;" +
                "}).filter(Boolean);";

            int prevCount = -1;
            List<String> captured = List.of();
            for (int i = 0; i < 10; i++) {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) js.executeScript(extractScript);
                int count = items != null ? items.size() : 0;
                System.out.println("[WhoScored] " + team.getName() + " - jugadores cargados: " + count);
                if (items != null && count > 0) captured = items;
                if (count > 0 && count == prevCount) break;
                prevCount = count;
            }

            for (String entry : captured) {
                String[] parts = entry.split("\\|\\|\\|", 3);
                if (parts.length < 3 || parts[1].trim().isEmpty()) continue;

                String playerId = "ws_" + extractPlayerIdFromUrl(parts[0].trim());
                if (seenPlayerIds.contains(playerId)) continue;
                seenPlayerIds.add(playerId);

                Player p = new Player();
                p.setId(playerId);
                p.setUrl(parts[0].trim());
                p.setName(parts[1].trim().replaceAll("^\\d+\\s*", "").trim());
                p.setPosition(parts[2].trim());
                p.setTeam(team.getName());
                p.setLeague(team.getLeague());
                players.add(p);
            }
        } catch (Exception e) {
            System.err.println("[WhoScored] Error scrapeando " + team.getName() + ": " + e.getMessage());
        }
        return players;
    }

    private String extractPlayerIdFromUrl(String href) {
        try {
            String[] parts = href.split("/[Pp]layers/");
            if (parts.length > 1) return parts[1].split("/")[0];
        } catch (Exception ignored) {}
        return String.valueOf(Math.abs(href.hashCode()));
    }

    // Extrae filas de un contenedor específico (statistics-table-{home|away}-{tab}).
    // Formato de fila: playerHref|||teamField|||stat1=val1|||stat2=val2...
    private static final String EXTRACT_CONTAINER_FN =
        "function extractContainer(containerId, teamField) {" +
        "  var container = document.querySelector('#' + containerId);" +
        "  if (!container) return [];" +
        "  var table = container.querySelector('table');" +
        "  if (!table) return [];" +
        "  var cols = Array.from(table.querySelectorAll('thead th')).map(function(th, i) {" +
        "    var s = th.getAttribute('data-stat-name');" +
        "    if (!s && th.textContent.trim() === 'Position') s = 'position';" +
        "    return s ? {i: i, stat: s} : null;" +
        "  }).filter(Boolean);" +
        "  return Array.from(table.querySelectorAll('tbody tr')).map(function(row) {" +
        "    var link = row.querySelector('a.player-link');" +
        "    if (!link) return null;" +
        "    var cells = Array.from(row.querySelectorAll('td'));" +
        "    var parts = [link.href, teamField];" +
        "    cols.forEach(function(c) {" +
        "      parts.push(c.stat + '=' + ((cells[c.i]||{}).textContent||'-').trim().replace(/\\s+/g,' '));" +
        "    });" +
        "    return parts.join('|||');" +
        "  }).filter(Boolean);" +
        "}";

    // Extrae filas del primer <table> dentro del div indicado, usando data-stat-name de los <th>
    private static final String EXTRACT_TAB_FN =
        "function extractTab(divId) {" +
        "  var container = document.querySelector('#' + divId);" +
        "  if (!container) return [];" +
        "  var table = container.querySelector('table');" +
        "  if (!table) return [];" +
        "  var ths = Array.from(table.querySelectorAll('thead th'));" +
        "  var cols = [];" +
        "  ths.forEach(function(th, i) {" +
        "    var stat = th.getAttribute('data-stat-name');" +
        "    if (!stat && th.textContent.trim() === 'Position') stat = 'position';" +
        "    if (stat) cols.push({i: i, stat: stat});" +
        "  });" +
        "  return Array.from(table.querySelectorAll('tbody tr')).map(function(row) {" +
        "    var link = row.querySelector('a.player-match-link');" +
        "    if (!link) return null;" +
        "    var cells = Array.from(row.querySelectorAll('td'));" +
        "    var opp = Array.from(link.childNodes)" +
        "      .filter(function(n){return n.nodeType===3;})" +
        "      .map(function(n){return n.textContent.trim();}).join('');" +
        "    var parts = [link.href, opp];" +
        "    cols.forEach(function(col) {" +
        "      var val = cells[col.i] ? cells[col.i].textContent.trim().replace(/\\s+/g,' ') : '-';" +
        "      parts.push(col.stat + '=' + val);" +
        "    });" +
        "    return parts.join('|||');" +
        "  }).filter(Boolean);" +
        "}";

    public List<PlayerMatchStats> scrapePlayerMatchStats(
            Player player, Set<String> existingMatchIds) {

        List<PlayerMatchStats> results = new ArrayList<>();
        if (player.getUrl() == null || player.getUrl().isBlank()) {
            System.out.println("[WhoScored] " + player.getName() + " no tiene URL, se omite");
            return results;
        }

        String matchStatsUrl = player.getUrl().replace("/show/", "/matchstatistics/");
        WebDriver driver = createDriver();
        try {
            driver.get(matchStatsUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("top-player-stats-summary-grid")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            boolean latestOnly = true;

            // 1. Summary tab — crea los objetos base
            List<String> summaryRows = pollTab(js, "player-matches-stats-summary", player.getName(), "Summary");
            if (latestOnly) summaryRows = summaryRows.isEmpty() ? summaryRows : summaryRows.subList(0, 1);

            // Early exit: si el último partido ya está en la DB no hace falta clickear más tabs
            if (!summaryRows.isEmpty()) {
                String latestMatchId = extractMatchIdFromUrl(summaryRows.get(0).split("\\|\\|\\|", 2)[0].trim());
                if (existingMatchIds.contains(latestMatchId)) {
                    System.out.println("[WhoScored] " + player.getName() + " - último partido ya en DB, se omite");
                    return results;
                }
            }

            Map<String, PlayerMatchStats> byMatchUrl = new LinkedHashMap<>();
            for (String row : summaryRows) {
                String[] p = row.split("\\|\\|\\|", -1);
                if (p.length < 3) continue;
                String matchUrl = p[0].trim();
                String matchId  = extractMatchIdFromUrl(matchUrl);
                if (existingMatchIds.contains(matchId)) continue;

                PlayerMatchStats s = new PlayerMatchStats();
                s.setId(matchId + "_" + player.getId());
                s.setMatchId(matchId);
                s.setPlayerId(player.getId());
                s.setMatchUrl(matchUrl);
                s.setOpponent(p[1].trim());
                applyStatPairs(s, p, 2);
                byMatchUrl.put(matchUrl, s);
            }

            // 2–4. Defensive / Offensive / Passing — enriquece los mismos objetos
            // tabHref, tabDivId, tabName
            String[][] tabs = {
                {"a[href='#player-matches-stats-defensive']", "player-matches-stats-defensive", "Defensive"},
                {"a[href='#player-matches-stats-offensive']", "player-matches-stats-offensive", "Offensive"},
                {"a[href='#player-matches-stats-passing']",   "player-matches-stats-passing",   "Passing"}
            };
            for (String[] tab : tabs) {
                try {
                    clickTab(driver, wait, js, tab[0], tab[1]);
                    List<String> tabRows = pollTab(js, tab[1], player.getName(), tab[2]);
                    if (latestOnly) tabRows = tabRows.isEmpty() ? tabRows : tabRows.subList(0, 1);
                    for (String row : tabRows) {
                        String[] p = row.split("\\|\\|\\|", -1);
                        if (p.length < 3) continue;
                        PlayerMatchStats s = byMatchUrl.get(p[0].trim());
                        if (s != null) applyStatPairs(s, p, 2);
                    }
                } catch (Exception e) {
                    System.err.println("[WhoScored] Tab " + tab[2] + " de " + player.getName() + ": " + e.getMessage());
                }
            }

            results.addAll(byMatchUrl.values());
            System.out.println("[WhoScored] " + player.getName() + " - partidos nuevos: " + results.size());
        } catch (Exception e) {
            System.err.println("[WhoScored] Error scrapeando partidos de " + player.getName() + ": " + e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }

    /**
     * Scrapes stats for ALL players in a single match.
     * The match URL comes from scrapeLeagueMatchUrls (/live/ format).
     * Player stats are on the /livestatistics/ sub-page, so we convert the URL before navigating.
     */
    public List<PlayerMatchStats> scrapeMatchPlayerStats(
            String matchUrl, Set<String> existingMatchIds) {

        String matchId = extractMatchIdFromUrl(matchUrl);
        if (existingMatchIds.contains(matchId)) {
            System.out.println("[WhoScored] Partido " + matchId + " ya en DB, se omite");
            return new ArrayList<>();
        }

        // /matches/{id}/live/{slug} → /matches/{id}/livestatistics/{slug}
        String statsUrl = matchUrl.replace("/live/", "/livestatistics/");

        WebDriver driver = createDriver();
        List<PlayerMatchStats> results = new ArrayList<>();
        Map<String, PlayerMatchStats> byPlayerId = new LinkedHashMap<>();

        try {
            driver.get(statsUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("live-player-stats")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Extract date and team names from match header
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) js.executeScript(
                "var dateEl = Array.from(document.querySelectorAll('.info-block dd'))" +
                "  .find(function(d){ return /\\d{2}-[A-Za-z]{3}-\\d{2}/.test(d.textContent); });" +
                "var homeEl = document.querySelector('[data-field=\"home\"] .team-name, [data-field=\"home\"] a.team-link');" +
                "var awayEl = document.querySelector('[data-field=\"away\"] .team-name, [data-field=\"away\"] a.team-link');" +
                "return {" +
                "  date:     dateEl ? dateEl.textContent.trim() : ''," +
                "  homeTeam: homeEl ? homeEl.textContent.trim() : ''," +
                "  awayTeam: awayEl ? awayEl.textContent.trim() : ''" +
                "};");

            String matchDate = meta != null ? String.valueOf(meta.getOrDefault("date", ""))     : "";
            String homeTeam  = meta != null ? String.valueOf(meta.getOrDefault("homeTeam", "")) : "";
            String awayTeam  = meta != null ? String.valueOf(meta.getOrDefault("awayTeam", "")) : "";
            System.out.println("[WhoScored] Partido " + matchId + ": " + homeTeam + " vs " + awayTeam + " (" + matchDate + ")");

            // [containerId, teamField, tabHref (null = already visible), label]
            String[][] extractions = {
                {"statistics-table-home-summary",   "home", null,                          "Home Summary"},
                {"statistics-table-away-summary",   "away", null,                          "Away Summary"},
                {"statistics-table-home-offensive", "home", "#live-player-home-offensive", "Home Offensive"},
                {"statistics-table-home-defensive", "home", "#live-player-home-defensive", "Home Defensive"},
                {"statistics-table-home-passing",   "home", "#live-player-home-passing",   "Home Passing"},
                {"statistics-table-away-offensive", "away", "#live-player-away-offensive", "Away Offensive"},
                {"statistics-table-away-defensive", "away", "#live-player-away-defensive", "Away Defensive"},
                {"statistics-table-away-passing",   "away", "#live-player-away-passing",   "Away Passing"},
            };

            for (String[] ext : extractions) {
                String containerId = ext[0];
                String teamField   = ext[1];
                String tabHref     = ext[2];
                String label       = ext[3];

                try {
                    if (tabHref != null) {
                        js.executeScript(
                            "document.querySelectorAll('[style*=\"z-index: 2147483647\"],[style*=\"z-index:2147483647\"]')" +
                            ".forEach(function(n){ n.remove(); });");
                        js.executeScript(
                            "var el = document.querySelector('a[href=\"" + tabHref + "\"]'); if (el) el.click();");
                        try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                    }

                    String script = EXTRACT_CONTAINER_FN +
                        " return extractContainer('" + containerId + "', '" + teamField + "');";
                    List<String> rows = pollMatchRows(js, script, matchId, label);
                    mergeIntoStatsMap(rows, byPlayerId, matchId, matchUrl, matchDate, homeTeam, awayTeam);

                } catch (Exception e) {
                    System.err.println("[WhoScored] " + label + " partido " + matchId + ": " + e.getMessage());
                }
            }

            results.addAll(byPlayerId.values());
            System.out.println("[WhoScored] Partido " + matchId + " - jugadores: " + results.size());

        } catch (Exception e) {
            System.err.println("[WhoScored] Error scrapeando partido " + matchId + ": " + e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }

    private List<String> pollMatchRows(JavascriptExecutor js, String script, String matchId, String label) {
        int prevCount = -1;
        List<String> captured = List.of();
        for (int i = 0; i < 20; i++) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(script);
            int count = items != null ? items.size() : 0;
            System.out.println("[WhoScored] Partido " + matchId + " [" + label + "] - filas: " + count);
            if (items != null && count > 0) captured = items;
            if (count > 0 && count == prevCount) break;
            prevCount = count;
        }
        return captured;
    }

    private void mergeIntoStatsMap(List<String> rows, Map<String, PlayerMatchStats> map,
            String matchId, String matchUrl, String matchDate, String homeTeam, String awayTeam) {
        for (String row : rows) {
            String[] parts = row.split("\\|\\|\\|", -1);
            if (parts.length < 2) continue;
            String playerHref = parts[0].trim();
            String teamField  = parts[1].trim();
            String playerId   = "ws_" + extractPlayerIdFromUrl(playerHref);
            String opponent   = "home".equals(teamField) ? awayTeam : homeTeam;

            PlayerMatchStats s = map.computeIfAbsent(playerId, pid -> {
                PlayerMatchStats stat = new PlayerMatchStats();
                stat.setId(matchId + "_" + pid);
                stat.setMatchId(matchId);
                stat.setPlayerId(pid);
                stat.setMatchUrl(matchUrl);
                stat.setDate(matchDate);
                stat.setOpponent(opponent);
                return stat;
            });
            applyStatPairs(s, parts, 2);
        }
    }

    private void clickTab(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                          String linkCss, String tabDivId) throws Exception {
        // Eliminar overlays de ads con z-index altísimo que bloquean el click
        js.executeScript(
            "document.querySelectorAll('[style*=\"z-index: 2147483647\"],[style*=\"z-index:2147483647\"]')" +
            ".forEach(function(n){ n.remove(); });");

        WebElement el = driver.findElement(By.cssSelector(linkCss));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        try {
            el.click();
        } catch (Exception ex) {
            // Fallback: click via JS con referencia al elemento (no depende de coordenadas)
            js.executeScript("arguments[0].click();", el);
        }

        // Esperar a que cargue al menos una fila en el div del tab
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#" + tabDivId + " tbody tr")));
    }

    private List<String> pollTab(JavascriptExecutor js, String tableId, String playerName, String tabName) {
        String script = EXTRACT_TAB_FN + " return extractTab('" + tableId + "');";
        int prevCount = -1;
        List<String> captured = List.of();
        for (int i = 0; i < 20; i++) {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(script);
            int count = items != null ? items.size() : 0;
            System.out.println("[WhoScored] " + playerName + " [" + tabName + "] - filas: " + count);
            if (items != null && count > 0) captured = items;
            if (count > 0 && count == prevCount) break;
            prevCount = count;
        }
        return captured;
    }

    private void applyStatPairs(PlayerMatchStats s, String[] parts, int from) {
        for (int i = from; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq < 0) continue;
            mapStat(s, parts[i].substring(0, eq).trim(), parts[i].substring(eq + 1).trim());
        }
    }

    private void mapStat(PlayerMatchStats s, String key, String val) {
        // Normalize to lowercase so both player-history names (camelCase) and
        // match-centre names (CamelCase / PascalCase) resolve to the same case.
        switch (key.toLowerCase()) {
            // Summary — player history names
            case "matchstarttime"                                  -> s.setDate(val);
            case "position"                                        -> s.setPosition(val);
            case "minsplayed"                                      -> trySetIntStat(s::setMinutesPlayed, val);
            case "goaltotal"                                       -> trySetDoubleStat(s::setGoals, val);
            case "assist"                                          -> trySetDoubleStat(s::setAssists, val);
            case "yellowcard"                                      -> trySetIntStat(s::setYellowCards, val);
            case "redcard"                                         -> trySetIntStat(s::setRedCards, val);
            case "shotstotal"                                      -> trySetDoubleStat(s::setShots, val);
            // passSuccess (player history) + PassSuccessInMatch (match centre)
            case "passsuccess", "passsuccessinmatch"               -> trySetDoubleStat(s::setPassSuccess, val);
            case "duelaerialwon"                                   -> trySetDoubleStat(s::setAerialsWon, val);
            case "rating"                                          -> trySetDoubleStat(s::setRating, val);
            // Defensive
            case "tackletotal", "tackletotalattempted"             -> trySetDoubleStat(s::setTackles, val);
            case "interceptionall"                                 -> trySetDoubleStat(s::setInterceptions, val);
            case "foulstotal"                                      -> trySetDoubleStat(s::setFoulsCommitted, val);
            case "clearancetotal"                                  -> trySetDoubleStat(s::setClearances, val);
            case "shotblocked"                                     -> trySetDoubleStat(s::setBlockedShots, val);
            case "savetotal", "saves"                              -> trySetDoubleStat(s::setSaves, val);
            // Offensive
            case "shotontarget", "shotsontarget"                   -> trySetDoubleStat(s::setShotsOnTarget, val);
            case "keypasstotal"                                    -> trySetDoubleStat(s::setKeyPasses, val);
            case "dribblewon"                                      -> trySetDoubleStat(s::setDribblesWon, val);
            case "foulstaken"                                      -> trySetDoubleStat(s::setFoulsWon, val);
            case "offsidegiven"                                    -> trySetDoubleStat(s::setOffsides, val);
            // Passing
            case "passtotal"                                       -> trySetDoubleStat(s::setTotalPasses, val);
            case "passlongballtotal", "longballtotal"              -> trySetDoubleStat(s::setLongBalls, val);
            case "passcrosstotal", "crosstotal"                    -> trySetDoubleStat(s::setCrosses, val);
            case "passthroughballtotal", "throughballtotal"        -> trySetDoubleStat(s::setThroughBalls, val);
        }
    }

    private String extractMatchIdFromUrl(String href) {
        try {
            String[] parts = href.split("/matches/");
            if (parts.length > 1) return parts[1].split("/")[0];
        } catch (Exception ignored) {}
        return String.valueOf(Math.abs(href.hashCode()));
    }

    private void trySetIntStat(java.util.function.Consumer<Integer> setter, String val) {
        try {
            String clean = val.replaceAll("[^0-9]", "");
            if (!clean.isEmpty()) setter.accept(Integer.parseInt(clean));
        } catch (NumberFormatException ignored) {}
    }

    private void trySetDoubleStat(java.util.function.Consumer<Double> setter, String val) {
        try {
            String clean = val.replaceAll("[^0-9.]", "");
            if (!clean.isEmpty()) setter.accept(Double.parseDouble(clean));
        } catch (NumberFormatException ignored) {}
    }

    private WebDriver createDriver() {
        String os = System.getProperty("os.name").toLowerCase();
        ChromeOptions options = new ChromeOptions();

        if (os.contains("win")) {
            System.out.println("[WhoScored] Detectado Windows. Usando configuración automática.");
        } else {
            System.out.println("[WhoScored] Detectado Linux. Aplicando rutas de Chromium.");
            System.setProperty("webdriver.chrome.driver", "/snap/bin/chromium.chromedriver");
            options.setBinary("/usr/bin/chromium-browser");
        }

        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-blink-features=AutomationControlled",
            "--remote-allow-origins=*" // Agregado para evitar errores de conexión en Windows
        );

        String userAgent = os.contains("win") 
            ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            : "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        
        options.addArguments("--user-agent=" + userAgent);

        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        return new ChromeDriver(options);
    }
}
