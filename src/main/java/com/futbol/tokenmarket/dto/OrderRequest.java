package com.futbol.tokenmarket.dto;

public class OrderRequest {

    private String playerId;
    private int quantity;

    public OrderRequest() {}

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
