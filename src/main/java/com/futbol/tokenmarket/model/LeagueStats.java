package com.futbol.tokenmarket.model;

public class LeagueStats {

    private String league;
    private Integer totalPlayers;

    public LeagueStats() {}

    public LeagueStats(String league, Integer totalPlayers) {
        this.league = league;
        this.totalPlayers = totalPlayers;
    }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public Integer getTotalPlayers() { return totalPlayers; }
    public void setTotalPlayers(Integer totalPlayers) { this.totalPlayers = totalPlayers; }
}
