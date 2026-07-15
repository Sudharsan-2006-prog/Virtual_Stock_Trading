package com.virtualstock.backend.repository;

import com.virtualstock.backend.entity.User;
import com.virtualstock.backend.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByUser(User user);
    Optional<Watchlist> findByUserAndStockSymbol(User user, String stockSymbol);
}
