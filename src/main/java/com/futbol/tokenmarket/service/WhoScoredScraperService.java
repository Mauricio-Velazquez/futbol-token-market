package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        try {
            driver.get(team.getUrl());
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.player-link")));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Extrae href, nombre y celdas de la fila de cada jugador en la tabla de stats
            String extractScript =
                "return Array.from(document.querySelectorAll('a.player-link')).map(function(a) {" +
                "  var row = a.closest('tr');" +
                "  if (!row) return null;" +
                "  var cells = Array.from(row.querySelectorAll('td'));" +
                "  var g = function(i) { return cells[i] ? cells[i].textContent.trim().replace(/\\s+/g,' ') : ''; };" +
                "  return a.href+'|||'+a.textContent.trim()+'|||'+g(2)+'|||'+g(3)+'|||'+g(4)+'|||'+g(5)+'|||'+g(6)+'|||'+g(9);" +
                "}).filter(function(x){return x!==null;});";

            int prevCount = -1;
            List<String> captured = List.of();
            for (int i = 0; i < 20; i++) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                @SuppressWarnings("unchecked")
                List<String> items = (List<String>) js.executeScript(extractScript);
                int count = items != null ? items.size() : 0;
                System.out.println("[WhoScored] " + team.getName() + " - jugadores cargados: " + count);
                if (items != null && count > 0) captured = items;
                if (count > 0 && count == prevCount) break;
                prevCount = count;
            }

            for (String entry : captured) {
                String[] parts = entry.split("\\|\\|\\|", -1);
                if (parts.length < 2 || parts[1].trim().isEmpty()) continue;
                Player p = new Player();
                p.setId("ws_" + extractPlayerIdFromUrl(parts[0].trim()));
                p.setName(parts[1].trim());
                p.setPosition(parts.length > 2 && !parts[2].trim().isEmpty() ? parts[2].trim() : "Unknown");
                // parts[3]=apps, parts[4]=mins, parts[5]=goals, parts[6]=assists, parts[7]=rating
                if (parts.length > 4) trySetMinutes(p, parts[4].trim());
                if (parts.length > 5) trySetDouble(p::setGoals, parts[5].trim());
                if (parts.length > 6) trySetDouble(p::setAssists, parts[6].trim());
                if (parts.length > 7) trySetDouble(p::setRating, parts[7].trim());
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

    private void trySetMinutes(Player p, String val) {
        try { p.setMinutes(Integer.parseInt(val.replaceAll("[^0-9]", ""))); } catch (NumberFormatException ignored) {}
    }

    private void trySetDouble(java.util.function.Consumer<Double> setter, String val) {
        try { setter.accept(Double.parseDouble(val.replaceAll("[^0-9.]", ""))); } catch (NumberFormatException ignored) {}
    }

    public void enrichPlayerWithStats(Player player) {
        System.out.println("enrichPlayerWithStats: delegado a scrapePlayersFromTeams para " + player.getName());
    }

    private WebDriver createDriver() {
        // Usar el ChromeDriver que viene incluido con el snap de Chromium
        System.setProperty("webdriver.chrome.driver", "/snap/bin/chromium.chromedriver");

        ChromeOptions options = new ChromeOptions();
        options.setBinary("/usr/bin/chromium-browser");
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-blink-features=AutomationControlled",
            "--user-agent=Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        return new ChromeDriver(options);
    }
}
