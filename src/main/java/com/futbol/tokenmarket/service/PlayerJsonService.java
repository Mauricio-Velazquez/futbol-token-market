package com.futbol.tokenmarket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.Player;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlayerJsonService {

    @Value("${app.players.path:bin/main/data/players.json}")
    private String playersFilePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Guardar lista de jugadores en players.json
     */
    public void savePlayersToJson(List<Player> players) {
        try {
            File file = new File(playersFilePath);
            
            // Crear directorio si no existe
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            
            // Guardar jugadores en JSON
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, players);
            
            System.out.println("[PlayerJsonService] ✅ Guardados " + players.size() + " jugadores en: " + file.getAbsolutePath());
            System.out.println("[PlayerJsonService] Tamaño archivo: " + file.length() + " bytes");
        } catch (Exception e) {
            System.err.println("[PlayerJsonService] ❌ Error guardando JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cargar lista de jugadores desde players.json
     */
    public List<Player> loadPlayersFromJson() {
        try {
            File file = new File(playersFilePath);
            if (!file.exists()) {
                System.out.println("[PlayerJsonService] Archivo no existe: " + playersFilePath);
                return new ArrayList<>();
            }
            
            String content = Files.readString(Paths.get(playersFilePath));
            List<Player> players = objectMapper.readValue(content, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Player.class));
            
            System.out.println("[PlayerJsonService] Cargados " + players.size() + " jugadores desde: " + playersFilePath);
            return players;
        } catch (Exception e) {
            System.err.println("[PlayerJsonService] Error cargando JSON: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Agregar jugadores a los existentes en JSON
     */
    public void appendPlayersToJson(List<Player> newPlayers) {
        try {
            List<Player> existing = loadPlayersFromJson();
            
            // Evitar duplicados por ID
            for (Player newPlayer : newPlayers) {
                if (existing.stream().noneMatch(p -> p.getId().equals(newPlayer.getId()))) {
                    existing.add(newPlayer);
                }
            }
            
            savePlayersToJson(existing);
        } catch (Exception e) {
            System.err.println("[PlayerJsonService] Error en append: " + e.getMessage());
        }
    }

}
