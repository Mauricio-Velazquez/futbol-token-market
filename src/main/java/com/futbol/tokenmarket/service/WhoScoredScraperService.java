package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.Team;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

@Service
public class WhoScoredScraperService {

    private static final Logger log = LoggerFactory.getLogger(WhoScoredScraperService.class);

    private static final String WHOSCORED_BASE = "https://www.whoscored.com";
    private static final String WS_PREFIX = "[WhoScored] ";
    private static final String SEPARATOR = "\\|\\|\\|";
    private static final String MATCH_PREFIX = "[WhoScored] Partido ";
    private static final String JS_CLICK_SCRIPT = "arguments[0].click();";

    private static final Map<String, String> LEAGUE_URLS = Map.of(
        "Premier League", WHOSCORED_BASE + "/regions/252/tournaments/2/england-premier-league",
        "La Liga",        WHOSCORED_BASE + "/regions/206/tournaments/4/spain-laliga",
        "Serie A",        WHOSCORED_BASE + "/regions/108/tournaments/5/italy-serie-a",
        "Bundesliga",     WHOSCORED_BASE + "/regions/81/tournaments/3/germany-bundesliga",
        "Ligue 1",        WHOSCORED_BASE + "/regions/74/tournaments/22/france-ligue-1"
    );

    private static class MatchInfo {
        final String matchId;
        final String matchUrl;
        final String matchDate;
        final String homeTeam;
        final String awayTeam;

        MatchInfo(String matchId, String matchUrl, String matchDate, String homeTeam, String awayTeam) {
            this.matchId = matchId;
            this.matchUrl = matchUrl;
            this.matchDate = matchDate;
            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
        }
    }

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

            // Navegar a la semana anterior (que tiene los partidos terminados):
            // 1. Abrir calendario → 2. Ir a la semana actual → 3. Retroceder una semana
            try {
                WebElement toggleBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("toggleCalendar")));
                js.executeScript(JS_CLICK_SCRIPT, toggleBtn);
                WebElement todayBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[class*='todayBtn']")));
                js.executeScript(JS_CLICK_SCRIPT, todayBtn);
                sleepMs(1000);
                WebElement prevBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.id("dayChangeBtn-prev")));
                js.executeScript(JS_CLICK_SCRIPT, prevBtn);
                sleepMs(1500);
            } catch (Exception e) {
                log.info(WS_PREFIX + "{} - error navegando al calendario: {}", leagueName, e.getMessage());
            }

            // Leer del DOM directamente: partidos con score real (no "-") y convertir /show/ → /live/
            // El JSON hypernova estático no se actualiza tras la navegación con el calendar
            String extractScript =
                "return Array.from(document.querySelectorAll('a[id^=\"scoresBtn-\"]'))" +
                "  .filter(function(a) {" +
                "    var spans = a.querySelectorAll('span');" +
                "    return spans.length >= 2 && spans[0].textContent.trim() !== '-';" +
                "  })" +
                "  .map(function(a) { return a.href.replace('/show/', '/live/'); });";

            List<String> items = pollItems(js, extractScript, 15, leagueName + " - partidos FT encontrados");
            if (items != null) matchUrls = new ArrayList<>(items);

            log.info(WS_PREFIX + "{} - total partidos FT: {}", leagueName, matchUrls.size());
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
            driver.get(leagueUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("sub-navigation")));
            WebElement teamStatsAnchor = driver.findElement(
                By.cssSelector("#sub-navigation a[href*='teamstatistics']")
            );
            String teamStatsUrl = teamStatsAnchor.getAttribute("href");
            if (teamStatsUrl == null) {
                throw new IllegalStateException("Team statistics URL not found in teamStatsAnchor element");
            }
            log.info(WS_PREFIX + "Navegando a: {}", teamStatsUrl);

            driver.get(teamStatsUrl);

            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("top-team-stats-summary-grid")
            ));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            String extractScript =
                "return Array.from(document.querySelectorAll(" +
                "  '#top-team-stats-summary-grid a.team-link'" +
                ")).map(a => a.href + '|||' + a.textContent.trim());";

            List<String> captured = pollItems(js, extractScript, 20, "Filas cargadas");
            parseTeamEntries(captured, teamUrls);
            log.info(WS_PREFIX + "Equipos encontrados: {}", teamUrls.size());
        } finally {
            driver.quit();
        }

        return teamUrls;
    }

    private void parseTeamEntries(List<String> captured, Map<String, String> teamUrls) {
        if (captured == null) return;
        for (String entry : captured) {
            String[] parts = entry.split(SEPARATOR, 2);
            if (parts.length == 2) {
                String href = parts[0].trim();
                String name = parts[1].trim().replaceAll("^\\d+\\.\\s*", "");
                if (!name.isEmpty()) {
                    teamUrls.computeIfAbsent(name, k -> href);
                }
            }
        }
    }

    public List<Player> scrapePlayersFromTeams(List<Team> teams) {
        List<Player> allPlayers = new ArrayList<>();
        WebDriver driver = createDriver();
        try {
            for (Team team : teams) {
                log.info(WS_PREFIX + "Scrapeando jugadores de: {}", team.getName());
                List<Player> teamPlayers = scrapePlayersFromTeam(driver, team);
                log.info(WS_PREFIX + "Jugadores encontrados en {}: {}", team.getName(), teamPlayers.size());
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

            List<String> captured = pollItems(js, extractScript, 10, team.getName() + " - jugadores cargados");
            parsePlayers(captured, team, seenPlayerIds, players);
        } catch (Exception e) {
            log.error(WS_PREFIX + "Error scrapeando {}: {}", team.getName(), e.getMessage());
        }
        return players;
    }

    private void parsePlayers(List<String> captured, Team team, Set<String> seenPlayerIds, List<Player> players) {
        if (captured == null) return;
        for (String entry : captured) {
            String[] parts = entry.split(SEPARATOR, 3);
            if (parts.length >= 3 && !parts[1].trim().isEmpty()) {
                String playerId = "ws_" + extractPlayerIdFromUrl(parts[0].trim());
                if (!seenPlayerIds.contains(playerId)) {
                    seenPlayerIds.add(playerId);
                    Player p = new Player();
                    p.setId(playerId);
                    p.setUrl(parts[0].trim());
                    p.setName(parts[1].trim().replaceAll("^\\d+\\s*", "").trim());
                    p.setPosition(parts[2].trim());
                    p.setTeamName(team.getName());
                    p.setTeam(team);
                    p.setLeague(team.getLeague());
                    players.add(p);
                }
            }
        }
    }

    private String extractPlayerIdFromUrl(String href) {
        try {
            String[] parts = href.split("/[Pp]layers/");
            if (parts.length > 1) return parts[1].split("/")[0];
        } catch (Exception ignored) {
            // fallback to hash
        }
        return String.valueOf(href.hashCode() & Integer.MAX_VALUE);
    }

    private static final String EXTRACT_CONTAINER_FN =
        "function extractContainer(containerId, teamField) {" +
        "  var container = document.querySelector('#' + containerId);" +
        "  if (!container) return [];" +
        "  var table = container.querySelector('table');" +
        "  if (!table) return [];" +
        "  function appendDerivedStats(parts, row, cells) {" +
        "    var firstCell = cells[0] || null;" +
        "    var metaCells = firstCell ? firstCell.querySelectorAll('.player-meta-data') : [];" +
        "    var position = '';" +
        "    for (var j = metaCells.length - 1; j >= 0; j--) {" +
        "      var txt = (metaCells[j] && metaCells[j].textContent) ? metaCells[j].textContent.trim().replace(/^,\\s*/, '') : '';" +
        "      if (/^[A-Za-z]{1,3}$/.test(txt)) { position = txt; break; }" +
        "    }" +
        "    if (position) parts.push('position=' + position);" +
        "    var rowText = firstCell ? firstCell.textContent.replace(/\\s+/g, ' ').trim() : '';" +
        "    var minuteMatch = rowText.match(/\\((\\d+)/);" +
        "    if (minuteMatch) {" +
        "      var minute = parseInt(minuteMatch[1], 10);" +
        "      if (!isNaN(minute)) {" +
        "        var minutesPlayed = /\\bSub\\b/i.test(rowText) ? Math.max(0, 90 - minute) : minute;" +
        "        parts.push('minsplayed=' + minutesPlayed);" +
        "      }" +
        "    }" +
        "    var incidentIcons = row.querySelectorAll('.incident-wrapper .incident-icon');" +
        "    var goals = 0;" +
        "    var assists = 0;" +
        "    var yellowCards = 0;" +
        "    var redCards = 0;" +
        "    incidentIcons.forEach(function(icon) {" +
        "      var cardType = icon.getAttribute('data-card-type');" +
        "      if (cardType === '31') yellowCards += 1;" +
        "      if (cardType === '32' || cardType === '33' || cardType === '34') redCards += 1;" +
        "      var hasGoal = icon.hasAttribute('data-event-satisfier-goalnormal') ||" +
        "        icon.hasAttribute('data-event-satisfier-goalpenaltyarea') ||" +
        "        icon.hasAttribute('data-event-satisfier-goalsetpiece') ||" +
        "        icon.hasAttribute('data-event-satisfier-goalleftfoot') ||" +
        "        icon.hasAttribute('data-event-satisfier-goalrightfoot') ||" +
        "        icon.hasAttribute('data-event-satisfier-goaheadgoal') ||" +
        "        icon.hasAttribute('data-event-satisfier-goalheader');" +
        "      if (hasGoal) goals += 1;" +
        "      var hasAssist = icon.hasAttribute('data-event-satisfier-assist') ||" +
        "        icon.hasAttribute('data-event-satisfier-intentionalassist') ||" +
        "        icon.hasAttribute('data-event-satisfier-assistother') ||" +
        "        icon.hasAttribute('data-event-satisfier-passkey');" +
        "      if (hasAssist) assists += 1;" +
        "    });" +
        "    if (goals > 0) parts.push('goaltotal=' + goals);" +
        "    if (assists > 0) parts.push('assist=' + assists);" +
        "    if (yellowCards > 0) parts.push('yellowcard=' + yellowCards);" +
        "    if (redCards > 0) parts.push('redcard=' + redCards);" +
        "  }" +
        "  function inferStatKey(label) {" +
        "    var normalized = (label || '').toLowerCase().replace(/[^a-z0-9]+/g, '');" +
        "    switch (normalized) {" +
        "      case 'position':" +
        "      case 'pos': return 'position';" +
        "      case 'minsplayed':" +
        "      case 'minutesplayed':" +
        "      case 'minplayed': return 'minsplayed';" +
        "      case 'goal':" +
        "      case 'goals': return 'goaltotal';" +
        "      case 'assist':" +
        "      case 'assists': return 'assist';" +
        "      case 'redcard':" +
        "      case 'redcards': return 'redcard';" +
        "      case 'saves':" +
        "      case 'save': return 'savetotal';" +
        "      case 'foulstotal':" +
        "      case 'foulstotals':" +
        "      case 'foulscommitted':" +
        "      case 'foulscommited': return 'foulstotal';" +
        "      case 'foulstaken':" +
        "      case 'fouled':" +
        "      case 'foulgiven':" +
        "      case 'foulswon': return 'foulstaken';" +
        "      default: return normalized || null;" +
        "    }" +
        "  }" +
        "  var cols = Array.from(table.querySelectorAll('thead th')).map(function(th, i) {" +
        "    var s = th.getAttribute('data-stat-name');" +
        "    if (!s) s = inferStatKey(th.textContent.trim());" +
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
        "    appendDerivedStats(parts, row, cells);" +
        "    return parts.join('|||');" +
        "  }).filter(Boolean);" +
        "}";

    private static final String EXTRACT_TAB_FN =
        "function extractTab(divId) {" +
        "  var container = document.querySelector('#' + divId);" +
        "  if (!container) return [];" +
        "  var table = container.querySelector('table');" +
        "  if (!table) return [];" +
        "  function inferStatKey(label) {" +
        "    var normalized = (label || '').toLowerCase().replace(/[^a-z0-9]+/g, '');" +
        "    switch (normalized) {" +
        "      case 'position':" +
        "      case 'pos': return 'position';" +
        "      case 'minsplayed':" +
        "      case 'minutesplayed':" +
        "      case 'minplayed': return 'minsplayed';" +
        "      case 'goal':" +
        "      case 'goals': return 'goaltotal';" +
        "      case 'assist':" +
        "      case 'assists': return 'assist';" +
        "      case 'redcard':" +
        "      case 'redcards': return 'redcard';" +
        "      case 'saves':" +
        "      case 'save': return 'savetotal';" +
        "      case 'foulstotal':" +
        "      case 'foulstotals':" +
        "      case 'foulscommitted':" +
        "      case 'foulscommited': return 'foulstotal';" +
        "      case 'foulstaken':" +
        "      case 'foulswon': return 'foulstaken';" +
        "      case 'opponent':" +
        "      case 'opp': return 'opponent';" +
        "      default: return normalized || null;" +
        "    }" +
        "  }" +
        "  var ths = Array.from(table.querySelectorAll('thead th'));" +
        "  var cols = [];" +
        "  var oppColIdx = -1;" +
        "  ths.forEach(function(th, i) {" +
        "    var stat = th.getAttribute('data-stat-name');" +
        "    if (!stat) stat = inferStatKey(th.textContent.trim());" +
        "    if (stat === 'opponent') { oppColIdx = i; }" +
        "    if (stat && stat !== 'opponent') cols.push({i: i, stat: stat});" +
        "  });" +
        "  return Array.from(table.querySelectorAll('tbody tr')).map(function(row) {" +
        "    var link = row.querySelector('a.player-match-link');" +
        "    if (!link) return null;" +
        "    var cells = Array.from(row.querySelectorAll('td'));" +
        "    var opp = '';" +
        "    if (oppColIdx >= 0 && cells[oppColIdx]) {" +
        "      opp = cells[oppColIdx].textContent.trim().replace(/\\s+/g,' ');" +
        "    }" +
        "    if (!opp || !opp.trim()) {" +
        "      opp = Array.from(link.childNodes)" +
        "        .filter(function(n){return n.nodeType===3;})" +
        "        .map(function(n){return n.textContent.trim();}).join('');" +
        "      if (!opp || !opp.trim()) opp = link.textContent.trim();" +
        "    }" +
        "    var parts = [link.href, opp];" +
        "    cols.forEach(function(col) {" +
        "      var val = cells[col.i] ? cells[col.i].textContent.trim().replace(/\\s+/g,' ') : '-';" +
        "      parts.push(col.stat + '=' + val);" +
        "    });" +
        "    return parts.join('|||');" +
        "  }).filter(Boolean);" +
        "}" ;

    public List<PlayerMatchStats> scrapePlayerMatchStats(
            Player player, Set<String> existingMatchIds) {

        List<PlayerMatchStats> results = new ArrayList<>();
        if (player.getUrl() == null || player.getUrl().isBlank()) {
            log.info(WS_PREFIX + "{} no tiene URL, se omite", player.getName());
            return results;
        }

        String matchStatsUrl = player.getUrl().replace("/show/", "/matchstatistics/");
        WebDriver driver = createDriver();
        try {
            driver.get(matchStatsUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("top-player-stats-summary-grid")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            List<String> summaryRows = pollTab(js, "player-matches-stats-summary", player.getName(), "Summary");
            if (!summaryRows.isEmpty()) summaryRows = summaryRows.subList(0, 1);

            if (!summaryRows.isEmpty()) {
                String latestMatchId = extractMatchIdFromUrl(summaryRows.get(0).split(SEPARATOR, 2)[0].trim());
                if (existingMatchIds.contains(latestMatchId)) {
                    log.info(WS_PREFIX + "{} - último partido ya en DB, se omite", player.getName());
                    return results;
                }
            }

            Map<String, PlayerMatchStats> byMatchUrl = buildStatsMap(summaryRows, existingMatchIds, player);

            String[][] tabs = {
                {"a[href='#player-matches-stats-defensive']", "player-matches-stats-defensive", "Defensive"},
                {"a[href='#player-matches-stats-offensive']", "player-matches-stats-offensive", "Offensive"},
                {"a[href='#player-matches-stats-passing']",   "player-matches-stats-passing",   "Passing"}
            };
            enrichStatsWithTabs(driver, wait, js, byMatchUrl, tabs, player.getName());

            results.addAll(byMatchUrl.values());
            log.info(WS_PREFIX + "{} - partidos nuevos: {}", player.getName(), results.size());
        } catch (Exception e) {
            log.error(WS_PREFIX + "Error scrapeando partidos de {}: {}", player.getName(), e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }

    private Map<String, PlayerMatchStats> buildStatsMap(
            List<String> summaryRows, Set<String> existingMatchIds, Player player) {
        Map<String, PlayerMatchStats> byMatchUrl = new LinkedHashMap<>();
        for (String row : summaryRows) {
            String[] p = row.split(SEPARATOR, -1);
            if (p.length >= 3) {
                String matchUrl = p[0].trim();
                String matchId  = extractMatchIdFromUrl(matchUrl);
                if (!existingMatchIds.contains(matchId)) {
                    PlayerMatchStats s = new PlayerMatchStats();
                    s.setId(matchId + "_" + player.getId());
                    s.setMatchId(matchId);
                    s.setPlayerId(player.getId());
                    s.setMatchUrl(matchUrl);
                    s.setOpponent(p[1].trim());
                    applyStatPairs(s, p, 2);
                    byMatchUrl.put(matchUrl, s);
                }
            }
        }
        return byMatchUrl;
    }

    private void enrichStatsWithTabs(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
            Map<String, PlayerMatchStats> byMatchUrl, String[][] tabs,
            String playerName) {
        for (String[] tab : tabs) {
            processTab(driver, wait, js, tab, byMatchUrl, playerName);
        }
    }

    private void processTab(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
            String[] tab, Map<String, PlayerMatchStats> byMatchUrl,
            String playerName) {
        try {
            clickTab(driver, wait, js, tab[0], tab[1]);
            List<String> tabRows = pollTab(js, tab[1], playerName, tab[2]);
            if (!tabRows.isEmpty()) tabRows = tabRows.subList(0, 1);
            for (String row : tabRows) {
                String[] p = row.split(SEPARATOR, -1);
                if (p.length >= 3) {
                    PlayerMatchStats s = byMatchUrl.get(p[0].trim());
                    if (s != null) applyStatPairs(s, p, 2);
                }
            }
        } catch (Exception e) {
            log.error(WS_PREFIX + "Tab {} de {}: {}", tab[2], playerName, e.getMessage());
        }
    }

    public List<PlayerMatchStats> scrapeMatchPlayerStats(
            String matchUrl, Set<String> existingMatchIds) {

        String matchId = extractMatchIdFromUrl(matchUrl);
        if (existingMatchIds.contains(matchId)) {
            log.info(MATCH_PREFIX + "{} ya en DB, se omite", matchId);
            return new ArrayList<>();
        }

        String statsUrl = matchUrl.replace("/live/", "/livestatistics/");

        WebDriver driver = createDriver();
        List<PlayerMatchStats> results = new ArrayList<>();
        Map<String, PlayerMatchStats> byPlayerId = new LinkedHashMap<>();

        try {
            driver.get(statsUrl);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("live-player-stats")));

            JavascriptExecutor js = (JavascriptExecutor) driver;

            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) js.executeScript(
                "var dateEl = Array.from(document.querySelectorAll('.info-block dd'))" +
                "  .find(function(d){ return /\\d{2}-[A-Za-z]{3}-\\d{2}/.test(d.textContent); });" +
                "var homeEl = document.querySelector('[data-field=\"home\"] .team-name, [data-field=\"home\"] a.team-link');" +
                "var awayEl = document.querySelector('[data-field=\"away\"] .team-name, [data-field=\"away\"] a.team-link');" +
                "if ((!homeEl || !homeEl.textContent.trim()) || (!awayEl || !awayEl.textContent.trim())) {" +
                "  var teamLinks = Array.from(document.querySelectorAll('a[href*=\"/teams/\"]')).map(function(a){ return a.textContent.trim(); }).filter(Boolean);" +
                "  if (teamLinks.length >= 2) { homeEl = { textContent: teamLinks[0] }; awayEl = { textContent: teamLinks[1] }; }" +
                "  else {" +
                "    var headerHome = document.querySelector('.team.home .team-name, .team.home a.team-link');" +
                "    var headerAway = document.querySelector('.team.away .team-name, .team.away a.team-link');" +
                "    if (headerHome && headerAway) { homeEl = headerHome; awayEl = headerAway; }" +
                "  }" +
                "}" +
                "return {" +
                "  date:     dateEl ? dateEl.textContent.trim() : ''," +
                "  homeTeam: homeEl ? homeEl.textContent.trim() : ''," +
                "  awayTeam: awayEl ? awayEl.textContent.trim() : ''" +
                "};");

            String matchDate = meta != null ? String.valueOf(meta.getOrDefault("date", ""))     : "";
            String homeTeam  = meta != null ? String.valueOf(meta.getOrDefault("homeTeam", "")) : "";
            String awayTeam  = meta != null ? String.valueOf(meta.getOrDefault("awayTeam", "")) : "";
            log.info(MATCH_PREFIX + "{}: {} vs {} ({})", matchId, homeTeam, awayTeam, matchDate);

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

            MatchInfo matchInfo = new MatchInfo(matchId, matchUrl, matchDate, homeTeam, awayTeam);
            for (String[] ext : extractions) {
                processExtraction(js, ext, matchInfo, byPlayerId);
            }

            // saves column removed — no goalkeeper saves enrichment

            results.addAll(byPlayerId.values());
            log.info(MATCH_PREFIX + "{} - jugadores: {}", matchId, results.size());

        } catch (Exception e) {
            log.error(WS_PREFIX + "Error scrapeando partido {}: {}", matchId, e.getMessage());
        } finally {
            driver.quit();
        }
        return results;
    }

    private void processExtraction(JavascriptExecutor js, String[] ext,
            MatchInfo matchInfo, Map<String, PlayerMatchStats> byPlayerId) {
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
                sleepMs(300);
            }
            String script = EXTRACT_CONTAINER_FN +
                " return extractContainer('" + containerId + "', '" + teamField + "');";
            List<String> rows = pollMatchRows(js, script, matchInfo.matchId, label);
            mergeIntoStatsMap(rows, byPlayerId, matchInfo);
        } catch (Exception e) {
            log.error(WS_PREFIX + "{} partido {}: {}", label, matchInfo.matchId, e.getMessage());
        }
    }

    private List<String> pollMatchRows(JavascriptExecutor js, String script, String matchId, String label) {
        int prevCount = -1;
        List<String> captured = List.of();
        for (int i = 0; i < 20; i++) {
            sleepMs(500);
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(script);
            int count = items != null ? items.size() : 0;
            log.info(MATCH_PREFIX + "{} [{}] - filas: {}", matchId, label, count);
            if (items != null && count > 0) captured = items;
            if (count > 0 && count == prevCount) break;
            prevCount = count;
        }
        return captured;
    }

    private void mergeIntoStatsMap(List<String> rows, Map<String, PlayerMatchStats> map, MatchInfo matchInfo) {
        for (String row : rows) {
            String[] parts = row.split(SEPARATOR, -1);
            if (parts.length < 2) continue;
            String playerHref = parts[0].trim();
            String teamField  = parts[1].trim();
            String playerId   = "ws_" + extractPlayerIdFromUrl(playerHref);
            String opponent   = "home".equals(teamField) ? matchInfo.awayTeam : matchInfo.homeTeam;

            PlayerMatchStats s = map.computeIfAbsent(playerId, pid -> {
                PlayerMatchStats stat = new PlayerMatchStats();
                stat.setId(matchInfo.matchId + "_" + pid);
                stat.setMatchId(matchInfo.matchId);
                stat.setPlayerId(pid);
                stat.setMatchUrl(matchInfo.matchUrl);
                stat.setPlayerUrl(playerHref);
                stat.setDate(matchInfo.matchDate);
                stat.setOpponent(opponent);
                return stat;
            });
            s.setPlayerUrl(playerHref);
            applyStatPairs(s, parts, 2);
        }
    }

    private void enrichGoalkeeperSaves(Map<String, PlayerMatchStats> byPlayerId) {
        // previously attempted to fill goalkeeper saves by scraping individual player pages
        // removed because `saves` column is not present consistently on WhoScored pages
    }

    private boolean isGoalkeeper(String position) {
        return position != null && position.toUpperCase().contains("GK");
    }

    private void clickTab(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
                          String linkCss, String tabDivId) {
        js.executeScript(
            "document.querySelectorAll('[style*=\"z-index: 2147483647\"],[style*=\"z-index:2147483647\"]')" +
            ".forEach(function(n){ n.remove(); });");

        WebElement el = driver.findElement(By.cssSelector(linkCss));
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", el);
        sleepMs(300);

        try {
            el.click();
        } catch (Exception ex) {
            // Fallback: click via JS cuando hay overlays que bloquean el click por coordenadas
            js.executeScript(JS_CLICK_SCRIPT, el);
        }

        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("#" + tabDivId + " tbody tr")));
    }

    private List<String> pollTab(JavascriptExecutor js, String tableId, String playerName, String tabName) {
        String script = EXTRACT_TAB_FN + " return extractTab('" + tableId + "');";
        int prevCount = -1;
        List<String> captured = List.of();
        for (int i = 0; i < 20; i++) {
            sleepMs(500);
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(script);
            int count = items != null ? items.size() : 0;
            log.info(WS_PREFIX + "{} [{}] - filas: {}", playerName, tabName, count);
            if (items != null && count > 0) captured = items;
            if (count > 0 && count == prevCount) break;
            prevCount = count;
        }
        return captured;
    }

    private List<String> pollItems(JavascriptExecutor js, String script, int maxAttempts, String label) {
        int prevCount = -1;
        List<String> captured = null;
        for (int i = 0; i < maxAttempts; i++) {
            sleepMs(500);
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) js.executeScript(script);
            int count = items != null ? items.size() : 0;
            log.info(WS_PREFIX + "{}: {}", label, count);
            if (items != null && count > 0) captured = items;
            if (count > 0 && count == prevCount) break;
            prevCount = count;
        }
        return captured;
    }

    private void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void applyStatPairs(PlayerMatchStats s, String[] parts, int from) {
        for (int i = from; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq < 0) continue;
            mapStat(s, parts[i].substring(0, eq).trim(), parts[i].substring(eq + 1).trim());
        }
    }

    private void mapStat(PlayerMatchStats s, String key, String val) {
        switch (normalizeStatKey(key)) {
            case "matchstarttime"                                  -> s.setDate(val);
            // position removed from model
            case "minsplayed", "minutesplayed", "minplayed"     -> trySetIntStat(s::setMinutesPlayed, val);
            case "goaltotal", "goal", "goals"                   -> trySetDoubleStat(s::setGoals, val);
            case "assist", "assists"                              -> trySetDoubleStat(s::setAssists, val);
            case "yellowcard"                                      -> trySetIntStat(s::setYellowCards, val);
            case "redcard", "redcards"                            -> trySetIntStat(s::setRedCards, val);
            case "shotstotal"                                      -> trySetDoubleStat(s::setShots, val);
            case "passsuccess", "passsuccessinmatch", "pa", "passaccuracy", "passcompletion" -> trySetDoubleStat(s::setPassSuccess, val);
            case "duelaerialwon"                                   -> trySetDoubleStat(s::setAerialsWon, val);
            case "rating"                                          -> trySetDoubleStat(s::setRating, val);
            case "tackletotal", "tackletotalattempted", "totaltackles", "tackles", "tacklewontotal" ->
                trySetDoubleStat(s::setTackles, val);
            case "interceptionall", "interceptions"               -> trySetDoubleStat(s::setInterceptions, val);
            case "foulstotal", "foulscommitted", "foulscommited", "foulcommitted", "fouls" ->
                trySetDoubleStat(s::setFoulsCommitted, val);
            case "clearancetotal", "clearances"                   -> trySetDoubleStat(s::setClearances, val);
            case "shotblocked", "blockedshots"                    -> trySetDoubleStat(s::setBlockedShots, val);
            // saves mapping removed — not persisted
            case "shotontarget", "shotsontarget"                   -> trySetDoubleStat(s::setShotsOnTarget, val);
            case "keypasstotal"                                    -> trySetDoubleStat(s::setKeyPasses, val);
            case "dribblewon"                                      -> trySetDoubleStat(s::setDribblesWon, val);
            // foulsWon removed from model
            case "offsidegiven", "offsides"                       -> trySetDoubleStat(s::setOffsides, val);
            case "passtotal", "passes", "totalpasses"           -> trySetDoubleStat(s::setTotalPasses, val);
            case "passlongballtotal", "longballtotal"              -> trySetDoubleStat(s::setLongBalls, val);
            case "passcrosstotal", "crosstotal"                    -> trySetDoubleStat(s::setCrosses, val);
            case "passthroughballtotal", "throughballtotal"        -> trySetDoubleStat(s::setThroughBalls, val);
            default -> { /* unknown stat key, intentionally ignored */ }
        }
    }

    private String normalizeStatKey(String key) {
        return key == null ? "" : key.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String extractMatchIdFromUrl(String href) {
        try {
            // Handle URLs like /matches/123/show or /live/123
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("/(?:matches|live)/([^/]+)")
                .matcher(href);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {
            // fallback to hash
        }
        return String.valueOf(href.hashCode() & Integer.MAX_VALUE);
    }

    private void trySetIntStat(IntConsumer setter, String val) {
        try {
            String clean = val.replaceAll("\\D", "");
            if (!clean.isEmpty()) setter.accept(Integer.parseInt(clean));
        } catch (NumberFormatException ignored) {
            // non-numeric value, skip
        }
    }

    private void trySetDoubleStat(DoubleConsumer setter, String val) {
        try {
            String clean = val.replaceAll("[^0-9.]", "");
            if (!clean.isEmpty()) setter.accept(Double.parseDouble(clean));
        } catch (NumberFormatException ignored) {
            // non-numeric value, skip
        }
    }

    private WebDriver createDriver() {
        String os = System.getProperty("os.name").toLowerCase();
        ChromeOptions options = new ChromeOptions();

        if (os.contains("win")) {
            log.info(WS_PREFIX + "Detectado Windows. Usando configuración automática.");
        } else {
            log.info(WS_PREFIX + "Detectado Linux. Aplicando rutas de Chromium.");
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
            "--remote-allow-origins=*"
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
