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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class WhoScoredScraperService {

    private static final String WHOSCORED_BASE = "https://www.whoscored.com";

    @Value("${whoscored.enrichment.enabled:false}")
    private boolean enrichmentEnabled;

    @Value("${whoscored.parallel-threads:3}")
    private int parallelThreads;

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
                
                // Enriquecer con datos del perfil individual (solo si está habilitado)
                if (enrichmentEnabled) {
                    System.out.println("[WhoScored] Iniciando enriquecimiento paralelo con " + parallelThreads + " threads");
                    enrichPlayersInParallel(teamPlayers);
                }
                
                allPlayers.addAll(teamPlayers);
            }
        } finally {
            driver.quit();
        }
        return allPlayers;
    }

    private void enrichPlayersInParallel(List<Player> players) {
        ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Player player : players) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                WebDriver enrichDriver = null;
                try {
                    enrichDriver = createDriver();
                    enrichPlayerWithProfileData(enrichDriver, player);
                } catch (Exception e) {
                    System.err.println("[WhoScored] Error enriqueciendo " + player.getName() + ": " + e.getMessage());
                } finally {
                    if (enrichDriver != null) {
                        try { enrichDriver.quit(); } catch (Exception ignored) {}
                    }
                }
            }, executor);
            futures.add(future);
        }

        // Esperar a que todos terminen
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        System.out.println("[WhoScored] Enriquecimiento paralelo completado");
    }

    private List<Player> scrapePlayersFromTeam(WebDriver driver, Team team) {
        List<Player> players = new ArrayList<>();
        Set<String> seenPlayerIds = new HashSet<>();
        try {
            driver.get(team.getUrl());
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a.player-link")));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            // Extrae TODAS las celdas para debuggear qué índices contienen qué datos
            String extractScript =
                "var table = document.querySelector('#top-player-stats-summary-grid tbody');" +
                "if (!table) return [];" +
                "return Array.from(table.querySelectorAll('tr')).map(function(row) {" +
                "  var playerLink = row.querySelector('a.player-link');" +
                "  if (!playerLink) return null;" +
                "  var cells = Array.from(row.querySelectorAll('td'));" +
                "  var getName = function() { return playerLink.textContent.trim(); };" +
                "  var getPos = function() {" +
                "    var meta = cells[0] ? cells[0].querySelector('.player-meta-data:last-child') : null;" +
                "    return meta ? meta.textContent.trim().replace(/^,\\s*/, '') : 'Unknown';" +
                "  };" +
                "  var getCellText = function(i) { return cells[i] ? cells[i].textContent.trim().replace(/\\s+/g,' ') : ''; };" +
                "  var allCells = [];" +
                "  for (var i = 1; i < Math.min(15, cells.length); i++) { allCells.push(getCellText(i)); }" +
                "  return playerLink.href + '|||' + getName() + '|||' + getPos() + '|||' + allCells.join('|||');" +
                "}).filter(function(x){return x!==null;});";

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
                String[] parts = entry.split("\\|\\|\\|", -1);
                if (parts.length < 5 || parts[1].trim().isEmpty()) continue;
                
                String playerId = "ws_" + extractPlayerIdFromUrl(parts[0].trim());
                // Evitar duplicados
                if (seenPlayerIds.contains(playerId)) continue;
                seenPlayerIds.add(playerId);
                
                Player p = new Player();
                p.setId(playerId);
                // Limpiar nombre: remover números del inicio (ej: "1Erling Haaland" -> "Erling Haaland")
                String cleanName = parts[1].trim().replaceAll("^\\d+\\s*", "").trim();
                p.setName(cleanName);
                p.setPosition(parts[2].trim());
                
                // Mapeo correcto de índices desde JavaScript (basado en debug):
                // El split "|||" agrega 3 índices al inicio (URL, nombre, posición)
                // parts[7]=mins (2690), parts[8]=goals (24), parts[9]=assists (7), parts[16]=rating (7.49)
                if (parts.length > 7) trySetMinutes(p, parts[7].trim());      // mins
                if (parts.length > 8) trySetDouble(p::setGoals, parts[8].trim());      // goals
                if (parts.length > 9) trySetDouble(p::setAssists, parts[9].trim());    // assists
                if (parts.length > 16) trySetDouble(p::setRating, parts[16].trim());   // rating
                
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

    private void enrichPlayerWithProfileData(WebDriver driver, Player player) {
        try {
            // Construir URL del perfil individual desde el ID
            String profileUrl = WHOSCORED_BASE + "/players/" + player.getId().replace("ws_", "") + "/show";
            driver.get(profileUrl);
            
            // Esperar a que cargue la tabla de torneo
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("top-player-stats-summary-grid")));
            
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // Extraer datos de la tabla Summary del perfil personal del jugador
            String extractScript =
                "var table = document.querySelector('#top-player-stats-summary-grid tbody');" +
                "if (!table) return null;" +
                "var rows = Array.from(table.querySelectorAll('tr'));" +
                "var premierLeagueRow = rows.find(r => r.textContent.includes('Premier League'));" +
                "if (!premierLeagueRow) return null;" +
                "var cells = Array.from(premierLeagueRow.querySelectorAll('td'));" +
                "return {" +
                "  apps: cells[1] ? cells[1].textContent.trim() : ''," +
                "  mins: cells[2] ? cells[2].textContent.trim() : ''," +
                "  goals: cells[3] ? cells[3].textContent.trim() : ''," +
                "  assists: cells[4] ? cells[4].textContent.trim() : ''," +
                "  rating: cells[11] ? cells[11].textContent.trim() : ''" +
                "};";
            
            Object result = js.executeScript(extractScript);
            if (result instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> stats = (java.util.Map<String, Object>) result;
                
                System.out.println("[WhoScored] Datos del perfil de " + player.getName() + ": " + stats);
                
                // Los datos ya están en el Player desde la tabla de equipo
                // Aquí podríamos actualizar si queremos priorizar los datos del perfil personal
                System.out.println("[WhoScored] Perfil completado para " + player.getName());
            }
        } catch (Exception e) {
            System.err.println("[WhoScored] Error enriqueciendo perfil de " + player.getName() + ": " + e.getMessage());
        }
    }

    public void enrichPlayerWithStats(Player player) {
        System.out.println("enrichPlayerWithStats: delegado a enriquecimiento en scrapePlayersFromTeams para " + player.getName());
    }

    private WebDriver createDriver() {
        // WebDriverManager automatically downloads and manages ChromeDriver
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments(
            "--headless=new",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-blink-features=AutomationControlled",
            "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        );
        options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        return new ChromeDriver(options);
    }
}
