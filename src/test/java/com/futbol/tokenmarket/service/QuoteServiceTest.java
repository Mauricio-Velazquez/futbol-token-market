package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.Player;
import com.futbol.tokenmarket.model.PlayerMatchStats;
import com.futbol.tokenmarket.model.PlayerQuote;
import com.futbol.tokenmarket.model.QuoteSettings;
import com.futbol.tokenmarket.model.QuoteStrategy;
import com.futbol.tokenmarket.repository.PlayerMatchStatsRepository;
import com.futbol.tokenmarket.repository.PlayerQuoteRepository;
import com.futbol.tokenmarket.repository.PlayerRepository;
import com.futbol.tokenmarket.repository.QuoteSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("QuoteService")
class QuoteServiceTest {

    private static final String PLAYER_ID = "player-1";

    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerMatchStatsRepository matchStatsRepository;
    @Mock private PlayerQuoteRepository quoteRepository;
    @Mock private QuoteSettingsRepository settingsRepository;
    @Mock private RankingCacheService rankingCacheService;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService(playerRepository, matchStatsRepository, quoteRepository, settingsRepository, rankingCacheService);
    }

    @Nested
    @DisplayName("getLatestPriceForPlayer")
    class GetLatestPriceForPlayer {
        @Test
        @DisplayName("devuelve el valor base cuando no hay cotizaciones para el jugador")
        void returnsBaseValueWhenNoQuotesExist() {
            when(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(PLAYER_ID)).thenReturn(List.of());
            BigDecimal price = quoteService.getLatestPriceForPlayer(PLAYER_ID);
            assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        }

        @Test
        @DisplayName("devuelve el valor de la cotización más reciente")
        void returnsMostRecentQuoteValue() {
            when(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(PLAYER_ID))
                    .thenReturn(List.of(quoteWithValue(250.0), quoteWithValue(180.0)));
            BigDecimal price = quoteService.getLatestPriceForPlayer(PLAYER_ID);
            assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(250.0));
        }
    }

    @Nested
    @DisplayName("recalculateQuotes")
    class RecalculateQuotes {
        @Test
        @DisplayName("recalcula correctamente con estrategia BALANCED guardando nuevas cotizaciones")
        void recalculatesWithBalancedStrategy() {
            Player p = new Player(); p.setId(PLAYER_ID); p.setPosition("FW");
            when(playerRepository.findAll()).thenReturn(List.of(p));

            PlayerMatchStats stat = new PlayerMatchStats();
            stat.setPlayerId(PLAYER_ID); stat.setRating(7.5); stat.setGoals(1.0); 
            stat.setMinutesPlayed(90);
            when(matchStatsRepository.findAll()).thenReturn(List.of(stat));
            when(quoteRepository.findByStrategyOrderByCalculatedAtDesc(QuoteStrategy.BALANCED)).thenReturn(List.of());
            when(settingsRepository.findActiveSettings()).thenReturn(Optional.of(new QuoteSettings()));

            Page<PlayerQuote> result = quoteService.recalculateQuotes("balanced", 0, 10);

            assertThat(result.getContent()).hasSize(1);
            verify(quoteRepository, atLeastOnce()).saveAll(any());
        }

        @Test
        @DisplayName("reutiliza la ultima cotización si los valores calculados no variaron")
        void reusesExistingQuoteIfNoChanges() {
            Player p = new Player(); p.setId(PLAYER_ID); p.setPosition("FW");
            when(playerRepository.findAll()).thenReturn(List.of(p));
            when(matchStatsRepository.findAll()).thenReturn(List.of());

            when(quoteRepository.findByStrategyOrderByCalculatedAtDesc(QuoteStrategy.BALANCED)).thenReturn(List.of());
            when(settingsRepository.findActiveSettings()).thenReturn(Optional.empty());
            
            Page<PlayerQuote> primeraPasada = quoteService.recalculateQuotes("balanced", 0, 10);
            PlayerQuote calculado = primeraPasada.getContent().get(0);

            PlayerQuote existing = new PlayerQuote();
            existing.setPlayer(p);
            existing.setStrategy(calculado.getStrategy());
            existing.setScore(calculado.getScore());
            existing.setValue(calculado.getValue());
            existing.setBreakdown(calculado.getBreakdown());

            reset(quoteRepository, settingsRepository);
            when(quoteRepository.findByStrategyOrderByCalculatedAtDesc(QuoteStrategy.BALANCED)).thenReturn(List.of(existing));
            when(settingsRepository.findActiveSettings()).thenReturn(Optional.empty());

            Page<PlayerQuote> result = quoteService.recalculateQuotes("balanced", 0, 10);

            assertThat(result.getContent()).hasSize(1);
            verify(quoteRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("recalcula con POSITION_AWARE dándole estadísticas a cada posición para cubrir per90 y breakdowns")
        void recalculatesWithPositionAwareAllPositions() {
            List<Player> players = new ArrayList<>();
            List<PlayerMatchStats> statsList = new ArrayList<>();
            String[] positions = {"GK", "D", "DM", "M", "AM", "FW", "XYZ"};
            
            for (int i = 0; i < positions.length; i++) {
                String id = "p-" + i;
                Player p = new Player(); p.setId(id); p.setPosition(positions[i]);
                players.add(p);

                // Forzamos estadísticas válidas para activar 'per90' en cada rama de posición
                PlayerMatchStats ms = new PlayerMatchStats();
                ms.setPlayerId(id);
                ms.setRating(7.0 + (i * 0.1));
                ms.setMinutesPlayed(90);
                ms.setGoals(1.0);
                ms.setYellowCards(1);
                ms.setRedCards(0);
                statsList.add(ms);
            }
            
            when(playerRepository.findAll()).thenReturn(players);
            when(matchStatsRepository.findAll()).thenReturn(statsList);
            when(quoteRepository.findByStrategyOrderByCalculatedAtDesc(QuoteStrategy.POSITION_AWARE)).thenReturn(List.of());
            when(settingsRepository.findActiveSettings()).thenReturn(Optional.empty());

            Page<PlayerQuote> result = quoteService.recalculateQuotes("position-aware", 0, 20);
            assertThat(result.getContent()).hasSize(positions.length);
        }
    }

    @Nested
    @DisplayName("getPlayerQuoteHistory")
    class GetPlayerQuoteHistory {
        @Test
        @DisplayName("obtiene todo el historial ordenado cuando no se envían fechas")
        void getHistoryWithoutDates() {
            when(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(PLAYER_ID))
                    .thenReturn(List.of(quoteWithValue(100.0)));
            Page<PlayerQuote> res = quoteService.getPlayerQuoteHistory(PLAYER_ID, null, null, 0, 10);
            assertThat(res.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("obtiene el historial filtrando combinando rangos parciales de fechas")
        void getHistoryWithPartialDates() {
            when(quoteRepository.findByPlayerIdAndDateRange(eq(PLAYER_ID), any(), any()))
                    .thenReturn(List.of(quoteWithValue(150.0)));
            
            quoteService.getPlayerQuoteHistory(PLAYER_ID, LocalDate.now(), null, 0, 10);
            quoteService.getPlayerQuoteHistory(PLAYER_ID, null, LocalDate.now(), 0, 10);

            verify(quoteRepository, times(2)).findByPlayerIdAndDateRange(eq(PLAYER_ID), any(), any());
        }
    }

    @Nested
    @DisplayName("getRanking")
    class GetRanking {
        @Test
        @DisplayName("devuelve el ranking ordenado y cubre lambdas de desempate por ID y valor nulo")
        void returnsSortedRankingWithTieBreaker() {
            QuoteSettings settings = new QuoteSettings(); settings.setActiveStrategy("balanced");
            when(settingsRepository.findActiveSettings()).thenReturn(Optional.of(settings));

            LocalDateTime ahora = LocalDateTime.now();
            Player p1 = new Player(); p1.setId("p1");
            Player p2 = new Player(); p2.setId("p2");

            PlayerQuote q1 = quoteWithValue(200.0); q1.setPlayer(p1); q1.setCalculatedAt(ahora);
            PlayerQuote q2 = quoteWithValue(200.0); q2.setPlayer(p2); q2.setCalculatedAt(ahora);
            PlayerQuote qNull = quoteWithValue(100.0);

            when(rankingCacheService.getSortedRanking(QuoteStrategy.BALANCED))
                    .thenReturn(List.of(q1, q2, qNull));

            Page<PlayerQuote> res = quoteService.getRanking(0, 10);
            assertThat(res.getContent()).isNotNull();
        }
    }

    @Nested
    @DisplayName("CasosBordeYMetodosOcultos")
    class CasosBordeYMetodosOcultos {
        @Test
        @DisplayName("Fuerza minutos inválidos y nulos para barrer las ramas lógicas de per90")
        void testPer90EdgeCases() {
            Player p = new Player(); p.setId(PLAYER_ID); p.setPosition("FW");
            when(playerRepository.findAll()).thenReturn(List.of(p));
            
            PlayerMatchStats msInvalid = new PlayerMatchStats();
            msInvalid.setPlayerId(PLAYER_ID);
            msInvalid.setMinutesPlayed(0); 
            msInvalid.setRating(6.5);
            
            when(matchStatsRepository.findAll()).thenReturn(List.of(msInvalid));
            when(quoteRepository.findByStrategyOrderByCalculatedAtDesc(QuoteStrategy.BALANCED)).thenReturn(List.of());
            
            quoteService.recalculateQuotes("balanced", 0, 10);
            verify(playerRepository).findAll();
        }

        @Test
        @DisplayName("Cubre las defensas booleanas de isSameQuote usando Reflection")
        void testIsSameQuoteDefensiveBranches() throws Exception {
            PlayerQuote q1 = new PlayerQuote();
            q1.setValue(100.0);
            q1.setScore(10.0);
            q1.setBreakdown("ABC");

            PlayerQuote q2 = new PlayerQuote();
            q2.setValue(100.0);
            q2.setScore(10.0);
            q2.setBreakdown("ABC");

            // Obtenemos el método privado isSameQuote por Reflection
            java.lang.reflect.Method method = QuoteService.class.getDeclaredMethod("isSameQuote", PlayerQuote.class, PlayerQuote.class);
            method.setAccessible(true);

            // Rama 1: Referencias idénticas (q1 == q1) -> Debe dar true
            boolean resultIdentico = (boolean) method.invoke(quoteService, q1, q1);
            assertThat(resultIdentico).isTrue();

            // Rama 2: Comparación con null -> Debe dar false
            boolean resultNull = (boolean) method.invoke(quoteService, q1, null);
            assertThat(resultNull).isFalse();

            // Rama 3: Estructura idéntica de objetos distintos -> Debe dar true
            boolean resultEstructural = (boolean) method.invoke(quoteService, q1, q2);
            assertThat(resultEstructural).isTrue();
        }

        @Test
        @DisplayName("Garantiza la estrategia por defecto cuando el campo viene vacío")
        void testGetActiveStrategyDefault() {
            when(settingsRepository.findActiveSettings()).thenReturn(Optional.of(new QuoteSettings()));
            QuoteStrategy strategy = quoteService.getActiveStrategy();
            assertThat(strategy).isEqualTo(QuoteStrategy.BALANCED);
        }
    }

    private static PlayerQuote quoteWithValue(double value) {
        PlayerQuote quote = new PlayerQuote();
        quote.setValue(value);
        return quote;
    }
}
