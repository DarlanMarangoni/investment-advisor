package br.com.investmentadvisor.domain.port.out;

import br.com.investmentadvisor.domain.model.Quote;

import java.util.List;
import java.util.Optional;

public interface QuoteRepositoryPort {
    void save(Quote quote);
    List<Quote> findByTicker(String ticker);
    List<Quote> findLatestByTicker(String ticker, int limit);
    Optional<Quote> findLatest(String ticker);
}