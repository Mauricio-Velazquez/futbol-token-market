package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.Player;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public PlayerRepository(ObjectMapper objectMapper, @Value("${app.players.path}") String dataPath) {
        this.objectMapper = objectMapper;
        this.dataPath = dataPath;
    }

    public List<Player> findAll() throws IOException {
        File file = new File(dataPath);
        if (!file.exists()) return new ArrayList<>();
        return objectMapper.readValue(file, new TypeReference<>() {});
    }

    public Optional<Player> findById(String id) throws IOException {
        return findAll().stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    public List<Player> findByLeague(String league) throws IOException {
        return findAll().stream()
                .filter(p -> p.getLeague().equalsIgnoreCase(league))
                .toList();
    }

    public Player save(Player player) throws IOException {
        List<Player> players = findAll();
        players.removeIf(p -> p.getId().equals(player.getId()));
        players.add(player);
        writeToFile(players);
        return player;
    }

    public boolean saveAll(List<Player> players) throws IOException {
        List<Player> existing = findAll();
        existing.addAll(players);
        writeToFile(existing);
        return true;
    }

    public synchronized boolean savePlayersForLeague(String league, List<Player> players) throws IOException {
        List<Player> existing = findAll();
        existing.removeIf(p -> p.getLeague().equalsIgnoreCase(league));
        existing.addAll(players);
        writeToFile(existing);
        return true;
    }

    public boolean deleteById(String id) throws IOException {
        List<Player> players = findAll();
        boolean removed = players.removeIf(p -> p.getId().equals(id));
        if (removed) writeToFile(players);
        return removed;
    }

    private void writeToFile(List<Player> players) throws IOException {
        File file = new File(dataPath);
        file.getParentFile().mkdirs();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, players);
    }
}
