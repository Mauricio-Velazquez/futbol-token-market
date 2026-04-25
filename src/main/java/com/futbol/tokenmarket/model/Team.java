package com.futbol.tokenmarket.model;

public class Team {

    private String name;
    private String url;
    private String league;

    public Team() {}

    public Team(String name, String url, String league) {
        this.name = name;
        this.url = url;
        this.league = league;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }
}
