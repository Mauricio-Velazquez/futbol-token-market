package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.model.Token;
import com.futbol.tokenmarket.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenService service;

    public TokenController(TokenService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Token>> getAll() throws IOException {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Token> getById(@PathVariable String id) throws IOException {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Token> create(@RequestBody Token token) throws IOException {
        return ResponseEntity.status(201).body(service.create(token));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Token> update(@PathVariable String id, @RequestBody Token token) throws IOException {
        return service.update(id, token)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws IOException {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
