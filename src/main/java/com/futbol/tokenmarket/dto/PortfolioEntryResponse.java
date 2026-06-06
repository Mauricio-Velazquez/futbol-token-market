package com.futbol.tokenmarket.dto;

import java.math.BigDecimal;

public class PortfolioEntryResponse {

    private String playerId;
    private String playerName;
    private int quantity;
    private BigDecimal currentPrice;
    private BigDecimal totalValue;

    public PortfolioEntryResponse(String playerId, String playerName,
                                   int quantity, BigDecimal currentPrice) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.quantity = quantity;
        this.currentPrice = currentPrice;
        this.totalValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getTotalValue() { return totalValue; }
}
