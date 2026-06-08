package com.futbol.tokenmarket.service;

import com.futbol.tokenmarket.model.PlayerQuote;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("QuoteService")
class QuoteServiceTest {

    private static final String PLAYER_ID = "player-1";

    @Mock private PlayerRepository playerRepository;
    @Mock private PlayerMatchStatsRepository matchStatsRepository;
    @Mock private PlayerQuoteRepository quoteRepository;
    @Mock private QuoteSettingsRepository settingsRepository;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService(playerRepository, matchStatsRepository,
                quoteRepository, settingsRepository);
    }

    @Nested
    @DisplayName("getLatestPriceForPlayer")
    class GetLatestPriceForPlayer {

        @Test
        @DisplayName("devuelve el valor base cuando no hay cotizaciones para el jugador")
        void returnsBaseValueWhenNoQuotesExist() {
            when(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(PLAYER_ID))
                    .thenReturn(List.of());

            BigDecimal price = quoteService.getLatestPriceForPlayer(PLAYER_ID);

            assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(100.0));
        }

        @Test
        @DisplayName("devuelve el valor de la cotización más reciente")
        void returnsMostRecentQuoteValue() {
            PlayerQuote latest = quoteWithValue(250.0);
            PlayerQuote older = quoteWithValue(180.0);
            when(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(PLAYER_ID))
                    .thenReturn(List.of(latest, older));

            BigDecimal price = quoteService.getLatestPriceForPlayer(PLAYER_ID);

            assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(250.0));
        }

        @Test
        @DisplayName("devuelve el valor exacto cuando solo hay una cotización")
        void returnsExactValueWhenSingleQuoteExists() {
            when(quoteRepository.findByPlayerIdOrderByCalculatedAtDesc(PLAYER_ID))
                    .thenReturn(List.of(quoteWithValue(123.45)));

            BigDecimal price = quoteService.getLatestPriceForPlayer(PLAYER_ID);

            assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(123.45));
        }
    }

    private static PlayerQuote quoteWithValue(double value) {
        PlayerQuote quote = new PlayerQuote();
        quote.setValue(value);
        return quote;
    }
}
