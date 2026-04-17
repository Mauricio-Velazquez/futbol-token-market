package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TokenRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public TokenRepository(ObjectMapper objectMapper, @Value("${app.data.path}") String dataPath) {
        this.objectMapper = objectMapper;
        this.dataPath = dataPath;
    }

    public List<Token> findAll() throws IOException {
        File file = new File(dataPath);
        if (!file.exists()) return new ArrayList<>();
        return objectMapper.readValue(file, new TypeReference<>() {});
    }

    public Optional<Token> findById(String id) throws IOException {
        return findAll().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public Token save(Token token) throws IOException {
        List<Token> tokens = findAll();
        tokens.removeIf(t -> t.getId().equals(token.getId()));
        tokens.add(token);
        persist(tokens);
        return token;
    }

    public boolean deleteById(String id) throws IOException {
        List<Token> tokens = findAll();
        boolean removed = tokens.removeIf(t -> t.getId().equals(id));
        if (removed) persist(tokens);
        return removed;
    }

    private void persist(List<Token> tokens) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataPath), tokens);
    }
}
