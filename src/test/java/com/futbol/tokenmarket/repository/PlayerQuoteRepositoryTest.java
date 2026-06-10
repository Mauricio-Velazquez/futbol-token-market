package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.QuoteStrategy;
import com.futbol.tokenmarket.model.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PlayerQuoteRepository.class)
@Tag("e2e")
@DisplayName("PlayerQuoteRepository")
class PlayerQuoteRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private PlayerQuoteRepository repository;

    @Test
    @DisplayName("Guarda y recupera cotizaciones por Player ID ordenadas por fecha descendente")
    void testFindByPlayerIdOrderByCalculatedAtDesc() {
        Player player = createAndPersistPlayer("p10", "Lamine Yamal");

        PlayerQuote quoteOld = createQuote(player, QuoteStrategy.BALANCED, LocalDateTime.now().minusDays(2));
        PlayerQuote quoteNew = createQuote(player, QuoteStrategy.BALANCED, LocalDateTime.now());

        repository.saveAll(List.of(quoteOld, quoteNew));

        List<PlayerQuote> result = repository.findByPlayerIdOrderByCalculatedAtDesc("p10");

        assertThat(result).hasSize(2);
        // Verifica el ordenamiento DESC por fecha de cálculo
        assertThat(result.get(0).getCalculatedAt()).isAfter(result.get(1).getCalculatedAt());
    }

    @Test
    @DisplayName("Filtra las cotizaciones correctamente por la estrategia indicada")
    void testFindByStrategyOrderByCalculatedAtDesc() {
        Player player = createAndPersistPlayer("p11", "Gavi");

        PlayerQuote balancedQuote = createQuote(player, QuoteStrategy.BALANCED, LocalDateTime.now());
        PlayerQuote positionQuote = createQuote(player, QuoteStrategy.POSITION_AWARE, LocalDateTime.now());

        repository.saveAll(List.of(balancedQuote, positionQuote));

        List<PlayerQuote> result = repository.findByStrategyOrderByCalculatedAtDesc(QuoteStrategy.BALANCED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStrategy()).isEqualTo(QuoteStrategy.BALANCED);
    }

    @Test
    @DisplayName("Filtra cotizaciones por Player ID dentro de un rango de fechas")
    void testFindByPlayerIdAndDateRange() {
        Player player = createAndPersistPlayer("p12", "Pedri");
        LocalDateTime now = LocalDateTime.now();

        PlayerQuote insideRange = createQuote(player, QuoteStrategy.BALANCED, now.minusHours(5));
        PlayerQuote outsideRange = createQuote(player, QuoteStrategy.BALANCED, now.minusDays(5));

        repository.saveAll(List.of(insideRange, outsideRange));

        List<PlayerQuote> result = repository.findByPlayerIdAndDateRange(
                "p12", now.minusDays(1), now.plusDays(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCalculatedAt()).isAfter(now.minusDays(1));
    }

    @Test
    @DisplayName("Filtra combinando Player ID y una estrategia específica")
    void testFindByPlayerIdAndStrategyOrderByCalculatedAtDesc() {
        Player player = createAndPersistPlayer("p13", "Frenkie de Jong");

        PlayerQuote targetQuote = createQuote(player, QuoteStrategy.POSITION_AWARE, LocalDateTime.now());
        PlayerQuote otherQuote = createQuote(player, QuoteStrategy.BALANCED, LocalDateTime.now());

        repository.saveAll(List.of(targetQuote, otherQuote));

        List<PlayerQuote> result = repository.findByPlayerIdAndStrategyOrderByCalculatedAtDesc(
                "p13", QuoteStrategy.POSITION_AWARE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStrategy()).isEqualTo(QuoteStrategy.POSITION_AWARE);
    }

    // --- Helpers ---

    private Player createAndPersistPlayer(String id, String name) {
        Team team = testEntityManager.persistAndFlush(new Team("Barcelona FC", "https://whoscored.com/barca", "La Liga"));
        Player p = new Player();
        p.setId(id);
        p.setName(name);
        p.setLeague("La Liga");
        p.setTeamName("Barcelona FC");
        p.setTeam(team);
        p.setPosition("M(C)");
        return testEntityManager.persistAndFlush(p);
    }

    private PlayerQuote createQuote(Player player, QuoteStrategy strategy, LocalDateTime calculatedAt) {
        PlayerQuote pq = new PlayerQuote();
        pq.setPlayer(player);
        pq.setStrategy(strategy);
        pq.setCalculatedAt(calculatedAt);
        pq.setValue(150.0); 
        pq.setScore(7.5); 
        
        return pq;
    }
}