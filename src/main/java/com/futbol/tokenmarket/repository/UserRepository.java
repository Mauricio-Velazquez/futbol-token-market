package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<User> findByUsername(String username) {
        List<User> results = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class)
            .setParameter("username", username)
            .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public User save(User user) {
        return em.merge(user);
    }
}
