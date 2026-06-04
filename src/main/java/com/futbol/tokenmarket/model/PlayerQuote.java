package com.futbol.tokenmarket.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Entity
public class PlayerQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    private QuoteStrategy strategy;

    @Column(name = "quote_value", nullable = false)
    private Double quoteValue;

    @Column(nullable = false)
    private Double score;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Schema(description = "Desglose legible de las métricas que suman al score y al valor final")
    @Column(length = 4000)
    private String breakdown;

    public PlayerQuote() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public QuoteStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(QuoteStrategy strategy) {
        this.strategy = strategy;
    }

    public Double getValue() {
        return quoteValue;
    }

    public void setValue(Double value) {
        this.quoteValue = value;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public String getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(String breakdown) {
        this.breakdown = breakdown;
    }
}