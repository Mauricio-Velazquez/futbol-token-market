package com.futbol.tokenmarket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class QuoteSettings {

    @Id
    private Long id;

    @Column(nullable = false)
    private String activeStrategy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public QuoteSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActiveStrategy() {
        return activeStrategy;
    }

    public void setActiveStrategy(String activeStrategy) {
        this.activeStrategy = activeStrategy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}