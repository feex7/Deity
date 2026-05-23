package com.blog.repository;

import com.blog.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    List<MarketData> findBySymbolAndTradeDateBetweenOrderByTradeDateAsc(String symbol, LocalDate start, LocalDate end);
    void deleteBySymbol(String symbol);
}