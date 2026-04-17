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
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrapiClient implements StockQuotePort {

    private final RestClient brapiRestClient;

    @Value("${app.brapi.token:}")
    private String token;

    @Value("${app.brapi.tickers-per-request:1}")
    private int tickersPerRequest;

    @Override
    public List<Quote> fetchQuotes(List<String> tickers) {
        List<Quote> result = new ArrayList<>();

        // Divide em lotes respeitando o limite do plano BrAPI
        for (int i = 0; i < tickers.size(); i += tickersPerRequest) {
            List<String> batch = tickers.subList(i, Math.min(i + tickersPerRequest, tickers.size()));
            result.addAll(fetchBatch(batch));
        }

        return result;
    }

    private List<Quote> fetchBatch(List<String> tickers) {
        String tickerParam = String.join(",", tickers);
        log.debug("Chamando BrAPI para: {}", tickerParam);

        BrapiResponse response = hasToken()
                ? brapiRestClient.get()
                        .uri("/api/quote/{tickers}?token={token}", tickerParam, token)
                        .retrieve()
                        .body(BrapiResponse.class)
                : brapiRestClient.get()
                        .uri("/api/quote/{tickers}", tickerParam)
                        .retrieve()
                        .body(BrapiResponse.class);

        if (response == null || response.results() == null) {
            log.warn("BrAPI retornou resposta vazia para: {}", tickerParam);
            return List.of();
        }

        return response.results().stream()
                .map(this::toQuote)
                .toList();
    }

    private boolean hasToken() {
        return token != null && !token.isBlank();
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