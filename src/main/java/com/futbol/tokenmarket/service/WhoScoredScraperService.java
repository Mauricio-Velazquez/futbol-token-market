package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
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

    public void enrichPlayerWithStats(Player player) {
        // Método placeholder — WhoScored requiere JS rendering para stats por jugador
        System.out.println("enrichPlayerWithStats: pendiente de implementar con Selenium para " + player.getName());
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
