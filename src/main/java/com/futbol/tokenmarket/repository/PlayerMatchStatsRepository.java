package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class PlayerMatchStatsRepository {

    private static final Logger log = LoggerFactory.getLogger(PlayerMatchStatsRepository.class);

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

    public Set<String> getExistingMatchIds() throws IOException {
        Set<String> ids = new HashSet<>();
        for (PlayerMatchStats s : findAll()) {
            ids.add(s.getMatchId());
        }
        return ids;
    }

    public void appendToTemp(List<PlayerMatchStats> stats, File tempFile) throws IOException {
        List<PlayerMatchStats> current = new ArrayList<>();
        if (tempFile.exists()) {
            current = objectMapper.readValue(tempFile, new TypeReference<>() {});
        }
        current.addAll(stats);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, current);
    }

    public synchronized void commitAndDeleteTemp(File tempFile) throws IOException {
        if (!tempFile.exists()) return;
        List<PlayerMatchStats> tempStats = objectMapper.readValue(tempFile, new TypeReference<>() {});
        List<PlayerMatchStats> existing = findAll();
        Set<String> existingIds = existing.stream().map(PlayerMatchStats::getId).collect(Collectors.toSet());
        for (PlayerMatchStats s : tempStats) {
            if (!existingIds.contains(s.getId())) existing.add(s);
        }
        writeToFile(existing);
        Files.delete(tempFile.toPath());
    }

    public File createLeagueTempFile(String league) {
        String safeName = league.replaceAll("[^a-zA-Z0-9]", "_");
        File dataDir = new File(dataPath).getParentFile();
        return new File(dataDir, "match_stats_" + safeName + ".tmp.json");
    }

    private void writeToFile(List<PlayerMatchStats> stats) throws IOException {
        File file = new File(dataPath);
        file.getParentFile().mkdirs();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, stats);
        log.info("[PlayerMatchStats] Guardados {} registros en: {}", stats.size(), file.getAbsolutePath());
    }
}
