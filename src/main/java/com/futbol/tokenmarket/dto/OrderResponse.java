package com.futbol.tokenmarket.dto;

import java.math.BigDecimal;

public class OrderResponse {

    private String type;
    private String playerId;
    private String playerName;
    private int quantity;
    private BigDecimal pricePerToken;
    private BigDecimal totalAmount;
    private BigDecimal walletBalanceAfter;

    public OrderResponse(String type, String playerId, String playerName,
                         int quantity, BigDecimal pricePerToken,
                         BigDecimal totalAmount, BigDecimal walletBalanceAfter) {
        this.type = type;
        this.playerId = playerId;
        this.playerName = playerName;
        this.quantity = quantity;
        this.pricePerToken = pricePerToken;
        this.totalAmount = totalAmount;
        this.walletBalanceAfter = walletBalanceAfter;
    }

    public String getType() { return type; }
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPricePerToken() { return pricePerToken; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getWalletBalanceAfter() { return walletBalanceAfter; }
}
