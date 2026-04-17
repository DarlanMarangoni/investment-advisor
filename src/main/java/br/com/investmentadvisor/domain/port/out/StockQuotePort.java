package br.com.investmentadvisor.domain.port.out;

import br.com.investmentadvisor.domain.model.Quote;

import java.util.List;

public interface StockQuotePort {
    List<Quote> fetchQuotes(List<String> tickers);
}