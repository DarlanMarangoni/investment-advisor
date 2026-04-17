package br.com.investmentadvisor.infrastructure.adapter.out.repository;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.port.out.QuoteRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryQuoteRepository implements QuoteRepositoryPort {

    private static final int MAX_QUOTES_PER_TICKER = 100;

    private final ConcurrentHashMap<String, LinkedList<Quote>> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Quote quote) {
        storage.compute(quote.ticker(), (ticker, quotes) -> {
            if (quotes == null) quotes = new LinkedList<>();
            quotes.addLast(quote);
            if (quotes.size() > MAX_QUOTES_PER_TICKER) quotes.removeFirst();
            return quotes;
        });
    }

    @Override
    public List<Quote> findByTicker(String ticker) {
        return List.copyOf(storage.getOrDefault(ticker, new LinkedList<>()));
    }

    @Override
    public List<Quote> findLatestByTicker(String ticker, int limit) {
        LinkedList<Quote> quotes = storage.getOrDefault(ticker, new LinkedList<>());
        int size = quotes.size();
        return List.copyOf(quotes).subList(Math.max(0, size - limit), size);
    }

    @Override
    public Optional<Quote> findLatest(String ticker) {
        LinkedList<Quote> quotes = storage.getOrDefault(ticker, new LinkedList<>());
        return quotes.isEmpty() ? Optional.empty() : Optional.of(quotes.getLast());
    }
}