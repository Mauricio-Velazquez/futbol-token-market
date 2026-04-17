package com.futbol.tokenmarket.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futbol.tokenmarket.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final ObjectMapper objectMapper;
    private final String dataPath;

    public UserRepository(ObjectMapper objectMapper, @Value("${app.users.path}") String dataPath) {
        this.objectMapper = objectMapper;
        this.dataPath = dataPath;
    }

    public Optional<User> findByUsername(String username) throws IOException {
        return findAll().stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    public User save(User user) throws IOException {
        List<User> users = findAll();
        users.removeIf(u -> u.getId().equals(user.getId()));
        users.add(user);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(dataPath), users);
        return user;
    }

    private List<User> findAll() throws IOException {
        File file = new File(dataPath);
        if (!file.exists()) return new ArrayList<>();
        return objectMapper.readValue(file, new TypeReference<>() {});
    }
}
