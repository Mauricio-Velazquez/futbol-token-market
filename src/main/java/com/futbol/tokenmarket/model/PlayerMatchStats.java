package com.futbol.tokenmarket.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class PlayerMatchStats {

    @Id
    private String id;          // matchId + "_" + playerId
    private String matchId;
    private String playerId;
    private String matchUrl;
    private String opponent;
    @Column(name = "match_date")
    private String date;
    @Transient
    private String playerUrl;

    // Summary
    private Integer minutesPlayed;
    private Double goals;
    private Double assists;
    private Integer yellowCards;
    private Integer redCards;
    private Double shots;
    private Double passSuccess;
    private Double aerialsWon;
    private Double rating;

    // Defensive
    private Double tackles;
    private Double interceptions;
    private Double foulsCommitted;
    private Double clearances;
    private Double blockedShots;

    // Offensive
    private Double shotsOnTarget;
    private Double keyPasses;
    private Double dribblesWon;
    private Double offsides;

    // Passing
    private Double totalPasses;
    private Double longBalls;
    private Double crosses;
    private Double throughBalls;

    public PlayerMatchStats() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getMatchUrl() { return matchUrl; }
    public void setMatchUrl(String matchUrl) { this.matchUrl = matchUrl; }

    public String getOpponent() { return opponent; }
    public void setOpponent(String opponent) { this.opponent = opponent; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPlayerUrl() { return playerUrl; }
    public void setPlayerUrl(String playerUrl) { this.playerUrl = playerUrl; }

    public Integer getMinutesPlayed() { return minutesPlayed; }
    public void setMinutesPlayed(Integer minutesPlayed) { this.minutesPlayed = minutesPlayed; }

    public Double getGoals() { return goals; }
    public void setGoals(Double goals) { this.goals = goals; }

    public Double getAssists() { return assists; }
    public void setAssists(Double assists) { this.assists = assists; }

    public Integer getYellowCards() { return yellowCards; }
    public void setYellowCards(Integer yellowCards) { this.yellowCards = yellowCards; }

    public Integer getRedCards() { return redCards; }
    public void setRedCards(Integer redCards) { this.redCards = redCards; }

    public Double getShots() { return shots; }
    public void setShots(Double shots) { this.shots = shots; }

    public Double getPassSuccess() { return passSuccess; }
    public void setPassSuccess(Double passSuccess) { this.passSuccess = passSuccess; }

    public Double getAerialsWon() { return aerialsWon; }
    public void setAerialsWon(Double aerialsWon) { this.aerialsWon = aerialsWon; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Double getTackles() { return tackles; }
    public void setTackles(Double tackles) { this.tackles = tackles; }

    public Double getInterceptions() { return interceptions; }
    public void setInterceptions(Double interceptions) { this.interceptions = interceptions; }

    public Double getFoulsCommitted() { return foulsCommitted; }
    public void setFoulsCommitted(Double foulsCommitted) { this.foulsCommitted = foulsCommitted; }

    public Double getClearances() { return clearances; }
    public void setClearances(Double clearances) { this.clearances = clearances; }

    public Double getBlockedShots() { return blockedShots; }
    public void setBlockedShots(Double blockedShots) { this.blockedShots = blockedShots; }

    public Double getShotsOnTarget() { return shotsOnTarget; }
    public void setShotsOnTarget(Double shotsOnTarget) { this.shotsOnTarget = shotsOnTarget; }

    public Double getKeyPasses() { return keyPasses; }
    public void setKeyPasses(Double keyPasses) { this.keyPasses = keyPasses; }

    public Double getDribblesWon() { return dribblesWon; }
    public void setDribblesWon(Double dribblesWon) { this.dribblesWon = dribblesWon; }

    public Double getOffsides() { return offsides; }
    public void setOffsides(Double offsides) { this.offsides = offsides; }

    public Double getTotalPasses() { return totalPasses; }
    public void setTotalPasses(Double totalPasses) { this.totalPasses = totalPasses; }

    public Double getLongBalls() { return longBalls; }
    public void setLongBalls(Double longBalls) { this.longBalls = longBalls; }

    public Double getCrosses() { return crosses; }
    public void setCrosses(Double crosses) { this.crosses = crosses; }

    public Double getThroughBalls() { return throughBalls; }
    public void setThroughBalls(Double throughBalls) { this.throughBalls = throughBalls; }
}
