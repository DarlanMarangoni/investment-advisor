package br.com.investmentadvisor.application.service;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.port.in.FetchQuotesUseCase;
import br.com.investmentadvisor.domain.port.out.QuoteRepositoryPort;
import br.com.investmentadvisor.domain.port.out.StockQuotePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FetchQuotesService implements FetchQuotesUseCase {

    private final StockQuotePort stockQuotePort;
    private final QuoteRepositoryPort quoteRepository;

    @Value("${app.stocks.tickers}")
    private String tickersConfig;

    @Override
    public void fetchAndStoreQuotes() {
        List<String> tickers = Arrays.asList(tickersConfig.split(","));
        log.info("Buscando cotações para {} ativos: {}", tickers.size(), tickers);

        List<Quote> quotes = stockQuotePort.fetchQuotes(tickers);
        quotes.forEach(quote -> {
            quoteRepository.save(quote);
            log.debug("Cotação salva: {} - R$ {}", quote.ticker(), quote.price());
        });

        log.info("{} cotações atualizadas com sucesso", quotes.size());
    }
}