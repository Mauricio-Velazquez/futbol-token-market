package com.futbol.tokenmarket.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Player {

    private String id;
    private String name;
    private String position;
    private Integer jerseyNumber;
    private String league;
    private String team;
    private String nationality;
    private Integer dateOfBirth;
    private String url;

    // Performance metrics
    private Integer minutes;
    private Double goals;
    private Double assists;
    private Double shots;
    private Double keyPasses;
    private Double dribbles;
    private Double tackles;
    private Double rating;

    public Player() {}

    public Player(String id, String name, String position, Integer jerseyNumber, String league, String team, String nationality, Integer dateOfBirth) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.jerseyNumber = jerseyNumber;
        this.league = league;
        this.team = team;
        this.nationality = nationality;
        this.dateOfBirth = dateOfBirth;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(Integer jerseyNumber) { this.jerseyNumber = jerseyNumber; }

    public String getLeague() { return league; }
    public void setLeague(String league) { this.league = league; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public Integer getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(Integer dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Integer getMinutes() { return minutes; }
    public void setMinutes(Integer minutes) { this.minutes = minutes; }

    public Double getGoals() { return goals; }
    public void setGoals(Double goals) { this.goals = goals; }

    public Double getAssists() { return assists; }
    public void setAssists(Double assists) { this.assists = assists; }

    public Double getShots() { return shots; }
    public void setShots(Double shots) { this.shots = shots; }

    public Double getKeyPasses() { return keyPasses; }
    public void setKeyPasses(Double keyPasses) { this.keyPasses = keyPasses; }

    public Double getDribbles() { return dribbles; }
    public void setDribbles(Double dribbles) { this.dribbles = dribbles; }

    public Double getTackles() { return tackles; }
    public void setTackles(Double tackles) { this.tackles = tackles; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
