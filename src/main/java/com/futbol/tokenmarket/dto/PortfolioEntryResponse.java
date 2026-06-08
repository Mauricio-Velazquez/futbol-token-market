package com.futbol.tokenmarket.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PortfolioEntryResponse {

    private String playerId;
    private String playerName;
    private int quantity;
    private BigDecimal currentPrice;
    private BigDecimal totalValue;
    private BigDecimal avgBuyPrice;
    private BigDecimal gain;
    private BigDecimal gainPercent;

    public PortfolioEntryResponse(String playerId, String playerName,
                                   int quantity, BigDecimal currentPrice,
                                   BigDecimal avgBuyPrice) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.quantity = quantity;
        this.currentPrice = currentPrice;
        this.totalValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
        this.avgBuyPrice = avgBuyPrice;
        this.gain = currentPrice.subtract(avgBuyPrice)
                .multiply(BigDecimal.valueOf(quantity));
        this.gainPercent = avgBuyPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : currentPrice.subtract(avgBuyPrice)
                        .divide(avgBuyPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
    }

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public int getQuantity() { return quantity; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getTotalValue() { return totalValue; }
    public BigDecimal getAvgBuyPrice() { return avgBuyPrice; }
    public BigDecimal getGain() { return gain; }
    public BigDecimal getGainPercent() { return gainPercent; }
}
