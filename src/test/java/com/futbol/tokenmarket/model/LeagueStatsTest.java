package com.futbol.tokenmarket.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LeagueStats")
class LeagueStatsTest {

    @Test
    @DisplayName("expone liga y total de jugadores via constructor y setters")
    void exposesFieldsThroughConstructorAndSetters() {
        LeagueStats stats = new LeagueStats("La Liga", 5);

        assertThat(stats.getLeague()).isEqualTo("La Liga");
        assertThat(stats.getTotalPlayers()).isEqualTo(5);

        stats.setLeague("Serie A");
        stats.setTotalPlayers(8);

        assertThat(stats.getLeague()).isEqualTo("Serie A");
        assertThat(stats.getTotalPlayers()).isEqualTo(8);
    }
}