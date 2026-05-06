package com.futbol.tokenmarket.model;

public class Player {

    private String id;
    private String name;
    private String position;
    private String team;
    private String league;
    private String url;

    // required by Jackson for deserialization
    public Player() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
