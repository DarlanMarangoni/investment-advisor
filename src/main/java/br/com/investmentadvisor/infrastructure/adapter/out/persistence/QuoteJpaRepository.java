package br.com.investmentadvisor.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuoteJpaRepository extends JpaRepository<QuoteEntity, Long> {

    @Query("SELECT q FROM QuoteEntity q WHERE q.ticker = :ticker ORDER BY q.timestamp DESC")
    List<QuoteEntity> findLatestByTicker(@Param("ticker") String ticker, Pageable pageable);

    @Query("SELECT q FROM QuoteEntity q WHERE q.ticker = :ticker ORDER BY q.timestamp DESC LIMIT 1")
    Optional<QuoteEntity> findTopByTickerOrderByTimestampDesc(@Param("ticker") String ticker);

    List<QuoteEntity> findAllByTickerOrderByTimestampAsc(String ticker);
}