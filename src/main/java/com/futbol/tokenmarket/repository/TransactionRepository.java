package com.futbol.tokenmarket.repository;

import com.futbol.tokenmarket.model.Transaction;
import com.futbol.tokenmarket.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class TransactionRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Transaction> findByUserOrderByCreatedAtDesc(User user) {
        return em.createQuery(
                "SELECT t FROM Transaction t WHERE t.user = :user ORDER BY t.createdAt DESC",
                Transaction.class)
            .setParameter("user", user)
            .getResultList();
    }

    @Transactional
    public Transaction save(Transaction transaction) {
        return em.merge(transaction);
    }
}
