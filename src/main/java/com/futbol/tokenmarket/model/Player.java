package com.futbol.tokenmarket.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Player {

    @Id
    private String id;
    private String name;
    private String position;
    private String league;
    private String url;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    public Player() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
}
