package br.com.investmentadvisor.infrastructure.adapter.out.persistence;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.port.out.QuoteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostgresQuoteRepository implements QuoteRepositoryPort {

    private final QuoteJpaRepository jpaRepository;

    @Override
    public void save(Quote quote) {
        jpaRepository.save(toEntity(quote));
    }

    @Override
    public List<Quote> findByTicker(String ticker) {
        return jpaRepository.findAllByTickerOrderByTimestampAsc(ticker)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Quote> findLatestByTicker(String ticker, int limit) {
        List<Quote> quotes = jpaRepository
                .findLatestByTicker(ticker, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();

        // A query retorna DESC; inverte para ordem cronológica (necessário para cálculos de análise técnica)
        List<Quote> chronological = new java.util.ArrayList<>(quotes);
        Collections.reverse(chronological);
        return Collections.unmodifiableList(chronological);
    }

    @Override
    public Optional<Quote> findLatest(String ticker) {
        return jpaRepository.findTopByTickerOrderByTimestampDesc(ticker)
                .map(this::toDomain);
    }

    private QuoteEntity toEntity(Quote quote) {
        return QuoteEntity.builder()
                .ticker(quote.ticker())
                .price(quote.price())
                .open(quote.open())
                .high(quote.high())
                .low(quote.low())
                .close(quote.close())
                .volume(quote.volume())
                .timestamp(quote.timestamp())
                .build();
    }

    private Quote toDomain(QuoteEntity entity) {
        return new Quote(
                entity.getTicker(),
                entity.getPrice(),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getClose(),
                entity.getVolume(),
                entity.getTimestamp()
        );
    }
}