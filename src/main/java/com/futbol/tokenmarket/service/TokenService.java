package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Token;
import com.futbol.tokenmarket.repository.TokenRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TokenService {

    private final TokenRepository repository;

    public TokenService(TokenRepository repository) {
        this.repository = repository;
    }

    public List<Token> getAll() throws IOException {
        return repository.findAll();
    }

    public Optional<Token> getById(String id) throws IOException {
        return repository.findById(id);
    }

    public Token create(Token token) throws IOException {
        token.setId(UUID.randomUUID().toString());
        return repository.save(token);
    }

    public Optional<Token> update(String id, Token updated) throws IOException {
        Optional<Token> existing = repository.findById(id);
        if (existing.isEmpty()) return Optional.empty();
        updated.setId(id);
        return Optional.of(repository.save(updated));
    }

    public boolean delete(String id) throws IOException {
        return repository.deleteById(id);
    }
}
