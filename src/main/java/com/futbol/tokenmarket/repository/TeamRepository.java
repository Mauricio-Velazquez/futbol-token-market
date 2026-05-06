package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TeamRepository {

    private static final Logger log = LoggerFactory.getLogger(TeamRepository.class);

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public TeamRepository(ObjectMapper objectMapper, @Value("${app.teams.path}") String dataPath) {
        this.objectMapper = objectMapper;
        this.dataPath = dataPath;
    }

    public List<Team> findAll() throws IOException {
        File file = new File(dataPath);
        if (!file.exists()) return new ArrayList<>();
        return objectMapper.readValue(file, new TypeReference<>() {});
    }

    public List<Team> findByLeague(String league) throws IOException {
        return findAll().stream()
                .filter(t -> t.getLeague().equalsIgnoreCase(league))
                .toList();
    }

    public List<Team> saveTeamsForLeague(String league, List<Team> teams) throws IOException {
        List<Team> existing = findAll();
        existing.removeIf(t -> t.getLeague().equalsIgnoreCase(league));
        existing.addAll(teams);
        writeToFile(existing);
        return teams;
    }

    private void writeToFile(List<Team> teams) throws IOException {
        File file = new File(dataPath);
        file.getParentFile().mkdirs();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, teams);
        log.info("[TeamRepository] Guardado {} equipos en: {}", teams.size(), file.getAbsolutePath());
    }
}
