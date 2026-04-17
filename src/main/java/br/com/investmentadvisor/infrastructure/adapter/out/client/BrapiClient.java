package br.com.investmentadvisor.infrastructure.adapter.out.client;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.port.out.StockQuotePort;
import br.com.investmentadvisor.infrastructure.adapter.out.client.dto.BrapiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrapiClient implements StockQuotePort {

    private final RestClient brapiRestClient;

    @Value("${app.brapi.token:}")
    private String token;

    @Override
    public List<Quote> fetchQuotes(List<String> tickers) {
        String tickerParam = String.join(",", tickers);
        log.debug("Chamando BrAPI para: {}", tickerParam);

        var uri = token != null && !token.isBlank()
                ? "/api/quote/{tickers}?token={token}"
                : "/api/quote/{tickers}";

        BrapiResponse response = token != null && !token.isBlank()
                ? brapiRestClient.get()
                        .uri(uri, tickerParam, token)
                        .retrieve()
                        .body(BrapiResponse.class)
                : brapiRestClient.get()
                        .uri(uri, tickerParam)
                        .retrieve()
                        .body(BrapiResponse.class);

        if (response == null || response.results() == null) {
            log.warn("BrAPI retornou resposta vazia");
            return List.of();
        }

        return response.results().stream()
                .map(this::toQuote)
                .toList();
    }

    private Quote toQuote(BrapiResponse.BrapiResult result) {
        return new Quote(
                result.symbol(),
                result.regularMarketPrice(),
                result.regularMarketOpen(),
                result.regularMarketDayHigh(),
                result.regularMarketDayLow(),
                result.regularMarketPrice(),
                result.regularMarketVolume(),
                LocalDateTime.now()
        );
    }
}