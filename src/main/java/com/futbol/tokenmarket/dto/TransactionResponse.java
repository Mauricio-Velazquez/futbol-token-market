package com.futbol.tokenmarket.dto;

import com.futbol.tokenmarket.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String playerId;
    private String playerName;
    private String type;
    private int quantity;
    private BigDecimal pricePerToken;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;

    public TransactionResponse(Transaction t) {
        this.id = t.getId();
        this.playerId = t.getPlayer().getId();
        this.playerName = t.getPlayer().getName();
        this.type = t.getType().name();
        this.quantity = t.getQuantity();
        this.pricePerToken = t.getPricePerToken();
        this.totalAmount = t.getTotalAmount();
        this.createdAt = t.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getType() { return type; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPricePerToken() { return pricePerToken; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
