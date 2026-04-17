package com.futbol.tokenmarket.model;

public class Token {

    private String id;
    private String playerName;
    private String team;
    private double price;
    private int availableSupply;

    public Token() {}

    public Token(String id, String playerName, String team, double price, int availableSupply) {
        this.id = id;
        this.playerName = playerName;
        this.team = team;
        this.price = price;
        this.availableSupply = availableSupply;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getAvailableSupply() { return availableSupply; }
    public void setAvailableSupply(int availableSupply) { this.availableSupply = availableSupply; }
}
