package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlayerMatchStatsRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public PlayerMatchStatsRepository(ObjectMapper objectMapper,
                                      @Value("${app.player-match-stats.path}") String dataPath) {
        this.objectMapper = objectMapper;
        this.dataPath = dataPath;
    }

    public List<PlayerMatchStats> findAll() throws IOException {
        File file = new File(dataPath);
        if (!file.exists()) return new ArrayList<>();
        return objectMapper.readValue(file, new TypeReference<>() {});
    }

    public List<PlayerMatchStats> findByPlayerId(String playerId) throws IOException {
        return findAll().stream()
                .filter(s -> playerId.equals(s.getPlayerId()))
                .toList();
    }

    public Set<String> getExistingMatchIds(String playerId) throws IOException {
        return findByPlayerId(playerId).stream()
                .map(PlayerMatchStats::getMatchId)
                .collect(Collectors.toSet());
    }

    public void saveAll(List<PlayerMatchStats> newStats) throws IOException {
        List<PlayerMatchStats> existing = findAll();
        Set<String> existingIds = existing.stream()
                .map(PlayerMatchStats::getId)
                .collect(Collectors.toSet());
        // Solo agrega los que no existen todavía
        for (PlayerMatchStats s : newStats) {
            if (!existingIds.contains(s.getId())) {
                existing.add(s);
            }
        }
        writeToFile(existing);
    }

    private void writeToFile(List<PlayerMatchStats> stats) throws IOException {
        File file = new File(dataPath);
        file.getParentFile().mkdirs();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, stats);
        System.out.println("[PlayerMatchStats] Guardados " + stats.size() + " registros en: " + file.getAbsolutePath());
    }
}
