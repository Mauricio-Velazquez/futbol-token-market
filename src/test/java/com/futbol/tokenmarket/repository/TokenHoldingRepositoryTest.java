package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.Team;
import com.futbol.tokenmarket.model.TokenHolding;
import com.futbol.tokenmarket.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TokenHoldingRepository.class)
@Tag("e2e")
@DisplayName("TokenHoldingRepository")
class TokenHoldingRepositoryTest {

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private TokenHoldingRepository repository;

    @Test
    @DisplayName("Busca un TokenHolding específico por Player y Owner")
    void testFindByPlayerAndOwner() {
        User owner = createAndPersistUser("u1", "usuario1");
        Player player = createAndPersistPlayer("p20", "Gavi");
        
        TokenHolding holding = createTokenHolding(player, owner, 10);
        testEntityManager.persistAndFlush(holding);

        // 1. Caso de éxito: Encuentra el registro existente
        Optional<TokenHolding> found = repository.findByPlayerAndOwner(player, owner);
        assertThat(found).isPresent();
        assertThat(found.get().getPlayer().getId()).isEqualTo("p20");

        // 2. Caso alternativo: No encuentra nada para un usuario sin holdings
        User anotherUser = createAndPersistUser("u2", "usuario2");
        Optional<TokenHolding> notFound = repository.findByPlayerAndOwner(player, anotherUser);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Recupera la lista completa de holdings pertenecientes a un Owner")
    void testFindByOwner() {
        User owner = createAndPersistUser("u3", "ownerTest");
        Player p1 = createAndPersistPlayer("p21", "Pedri");
        Player p2 = createAndPersistPlayer("p22", "Lamine Yamal");

        TokenHolding h1 = createTokenHolding(p1, owner, 5);
        TokenHolding h2 = createTokenHolding(p2, owner, 15);
        
        repository.saveAll(List.of(h1, h2));

        List<TokenHolding> results = repository.findByOwner(owner);
        
        assertThat(results).hasSize(2);
        assertThat(results).extracting(h -> h.getPlayer().getName())
                .containsExactlyInAnyOrder("Pedri", "Lamine Yamal");
    }

    @Test
    @DisplayName("Busca IDs de jugadores con holdings existentes y maneja listas vacías")
    void testFindPlayerIdsWithHoldings() {
        User owner = createAndPersistUser("u4", "holderUser");
        Player p1 = createAndPersistPlayer("p23", "Frenkie de Jong");

        TokenHolding h1 = createTokenHolding(p1, owner, 8);
        repository.save(h1);

        // Caso 1: Lista de entrada vacía (Cubre la bifurcación 'if (playerIds.isEmpty())')
        Set<String> emptyResult = repository.findPlayerIdsWithHoldings(Collections.emptyList());
        assertThat(emptyResult).isEmpty();

        // Caso 2: Lista con IDs existentes y no existentes
        List<String> searchIds = List.of("p23", "p24", "p999_inexistente");
        Set<String> foundIds = repository.findPlayerIdsWithHoldings(searchIds);

        // Solo "p23" tiene un TokenHolding real persistido
        assertThat(foundIds).hasSize(1).contains("p23");
    }

    // --- Helpers ---

    private User createAndPersistUser(String id, String username) {
        User user = new User(id, username, "hashed_password_123");
        return testEntityManager.persistAndFlush(user);
    }

    private Player createAndPersistPlayer(String id, String name) {
        Team team = testEntityManager.persistAndFlush(new Team("FC Barcelona", "https://whoscored.com/barca", "La Liga"));
        Player p = new Player();
        p.setId(id);
        p.setName(name);
        p.setLeague("La Liga");
        p.setTeamName("FC Barcelona");
        p.setTeam(team);
        p.setPosition("M(C)");
        return testEntityManager.persistAndFlush(p);
    }

    private TokenHolding createTokenHolding(Player player, User owner, int quantity) {
        return new TokenHolding(player, owner, quantity);
    }
}
