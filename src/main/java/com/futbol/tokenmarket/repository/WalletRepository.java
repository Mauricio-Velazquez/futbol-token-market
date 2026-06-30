package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.User;
import com.futbol.tokenmarket.model.Wallet;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class WalletRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Wallet> findByUser(User user) {
        List<Wallet> results = em.createQuery(
                "SELECT w FROM Wallet w WHERE w.user = :user", Wallet.class)
            .setParameter("user", user)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public Wallet save(Wallet wallet) {
        return em.merge(wallet);
    }
}
