package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.QuoteSettings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class QuoteSettingsRepository {

    public static final long SETTINGS_ID = 1L;

    @PersistenceContext
    private EntityManager em;

    public Optional<QuoteSettings> findActiveSettings() {
        return Optional.ofNullable(em.find(QuoteSettings.class, SETTINGS_ID));
    }

    @Transactional
    public QuoteSettings save(QuoteSettings settings) {
        if (settings.getId() == null) {
            settings.setId(SETTINGS_ID);
        }
        return em.merge(settings);
    }
}