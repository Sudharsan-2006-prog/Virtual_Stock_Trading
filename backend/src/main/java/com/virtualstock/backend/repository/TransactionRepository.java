package com.virtualstock.backend.repository;

import com.virtualstock.backend.entity.Transaction;
import com.virtualstock.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByTimestampDesc(User user);
}
