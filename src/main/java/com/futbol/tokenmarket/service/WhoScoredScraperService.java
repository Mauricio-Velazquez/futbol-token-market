package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class WhoScoredScraperService {

    private static final String WHOSCORED_URL = "https://www.whoscored.com";
    private static final int TIMEOUT = 10000;

    public void enrichPlayerWithStats(Player player) {
        try {
            // Buscar el jugador en WhoScored
            String searchUrl = WHOSCORED_URL + "/Search/SearchPlayer?term=" + player.getName().replace(" ", "+");
            Document doc = Jsoup.connect(searchUrl).timeout(TIMEOUT).get();
            
            // Extraer estadísticas del jugador
            extractPlayerStats(player, doc);
        } catch (IOException e) {
            // Si falla el scraping, dejamos los valores nulos
            System.err.println("No se pudo obtener datos de WhoScored para: " + player.getName());
        }
    }

    private void extractPlayerStats(Player player, Document doc) {
        try {
            // Buscar estadísticas en la página de búsqueda
            Elements rows = doc.select("table tbody tr");
            
            for (Element row : rows) {
                String playerName = row.select("td").get(1).text();
                
                // Si encontramos el jugador, extraer sus estadísticas
                if (playerName.equalsIgnoreCase(player.getName())) {
                    Elements statCells = row.select("td");
                    
                    // Extraer estadísticas básicas
                    if (statCells.size() > 5) {
                        try {
                            player.setMinutes(parseInteger(statCells.get(5).text()));
                            player.setGoals(parseDouble(statCells.get(6).text()));
                            player.setAssists(parseDouble(statCells.get(7).text()));
                            player.setShots(parseDouble(statCells.get(8).text()));
                            player.setKeyPasses(parseDouble(statCells.get(9).text()));
                            player.setTackles(parseDouble(statCells.get(10).text()));
                            player.setRating(parseDouble(statCells.get(11).text()));
                            
                            // Aproximación de dribbles si disponible
                            if (statCells.size() > 12) {
                                player.setDribbles(parseDouble(statCells.get(12).text()));
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            // Ignorar errores de parsing y usar valores por defecto
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error extrayendo estadísticas de WhoScored: " + e.getMessage());
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) return null;
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty() || value.equals("-")) return null;
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
