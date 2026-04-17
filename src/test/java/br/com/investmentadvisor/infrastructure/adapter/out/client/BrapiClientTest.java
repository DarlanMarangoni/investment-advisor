package br.com.investmentadvisor.infrastructure.adapter.out.client;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.infrastructure.adapter.out.client.dto.BrapiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrapiClientTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RestClient restClient;

    private BrapiClient client;

    @BeforeEach
    void setUp() {
        client = new BrapiClient(restClient);
        ReflectionTestUtils.setField(client, "token", "");
        ReflectionTestUtils.setField(client, "tickersPerRequest", 1);
    }

    @Test
    void fetchQuotes_semToken_deveChamarUriSemToken() {
        BrapiResponse response = brapiResponse("PETR4", 35.50);
        when(restClient.get()
                .uri("/api/quote/{tickers}", "PETR4")
                .retrieve()
                .body(BrapiResponse.class))
                .thenReturn(response);

        List<Quote> result = client.fetchQuotes(List.of("PETR4"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticker()).isEqualTo("PETR4");
        assertThat(result.get(0).price()).isEqualByComparingTo("35.50");
    }

    @Test
    void fetchQuotes_comToken_deveChamarUriComToken() {
        ReflectionTestUtils.setField(client, "token", "meu-token");
        BrapiResponse response = brapiResponse("PETR4", 35.50);
        when(restClient.get()
                .uri("/api/quote/{tickers}?token={token}", "PETR4", "meu-token")
                .retrieve()
                .body(BrapiResponse.class))
                .thenReturn(response);

        List<Quote> result = client.fetchQuotes(List.of("PETR4"));

        assertThat(result).hasSize(1);
    }

    @Test
    void fetchQuotes_comMultiplosTickers_deveFazerUmaRequisicaoPorTicker() {
        BrapiResponse r1 = brapiResponse("PETR4", 35.50);
        BrapiResponse r2 = brapiResponse("VALE3", 68.20);
        when(restClient.get().uri("/api/quote/{tickers}", "PETR4").retrieve().body(BrapiResponse.class)).thenReturn(r1);
        when(restClient.get().uri("/api/quote/{tickers}", "VALE3").retrieve().body(BrapiResponse.class)).thenReturn(r2);

        List<Quote> result = client.fetchQuotes(List.of("PETR4", "VALE3"));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Quote::ticker).containsExactly("PETR4", "VALE3");
    }

    @Test
    void fetchQuotes_comTickersPerRequest2_deveAgruparEmLotes() {
        ReflectionTestUtils.setField(client, "tickersPerRequest", 2);
        BrapiResponse batch = new BrapiResponse(List.of(
                result("PETR4", 35.50),
                result("VALE3", 68.20)
        ));
        when(restClient.get()
                .uri("/api/quote/{tickers}", "PETR4,VALE3")
                .retrieve()
                .body(BrapiResponse.class))
                .thenReturn(batch);

        List<Quote> result = client.fetchQuotes(List.of("PETR4", "VALE3"));

        // 2 resultados em uma única chamada = lote funcionou corretamente
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Quote::ticker).containsExactlyInAnyOrder("PETR4", "VALE3");
    }

    @Test
    void fetchQuotes_quandoApiRetornaNull_deveRetornarListaVazia() {
        when(restClient.get()
                .uri("/api/quote/{tickers}", "PETR4")
                .retrieve()
                .body(BrapiResponse.class))
                .thenReturn(null);

        List<Quote> result = client.fetchQuotes(List.of("PETR4"));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchQuotes_quandoResultadosNulos_deveRetornarListaVazia() {
        when(restClient.get()
                .uri("/api/quote/{tickers}", "PETR4")
                .retrieve()
                .body(BrapiResponse.class))
                .thenReturn(new BrapiResponse(null));

        List<Quote> result = client.fetchQuotes(List.of("PETR4"));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchQuotes_deveMapearTodosOsCamposCorretamente() {
        BrapiResponse response = new BrapiResponse(List.of(
                new BrapiResponse.BrapiResult(
                        "PETR4", "PETROBRAS PN", "Petróleo Brasileiro", "BRL",
                        BigDecimal.valueOf(35.50), BigDecimal.valueOf(34.80),
                        BigDecimal.valueOf(36.00), BigDecimal.valueOf(34.50),
                        BigDecimal.valueOf(35.20), 5000000L
                )
        ));
        when(restClient.get().uri(anyString(), (Object) any()).retrieve().body(BrapiResponse.class))
                .thenReturn(response);

        Quote quote = client.fetchQuotes(List.of("PETR4")).get(0);

        assertThat(quote.ticker()).isEqualTo("PETR4");
        assertThat(quote.price()).isEqualByComparingTo("35.50");
        assertThat(quote.open()).isEqualByComparingTo("34.80");
        assertThat(quote.high()).isEqualByComparingTo("36.00");
        assertThat(quote.low()).isEqualByComparingTo("34.50");
        assertThat(quote.volume()).isEqualTo(5000000L);
        assertThat(quote.timestamp()).isNotNull();
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private BrapiResponse brapiResponse(String ticker, double price) {
        return new BrapiResponse(List.of(result(ticker, price)));
    }

    private BrapiResponse.BrapiResult result(String ticker, double price) {
        BigDecimal p = BigDecimal.valueOf(price);
        return new BrapiResponse.BrapiResult(ticker, ticker, ticker, "BRL", p, p, p, p, p, 1000L);
    }
}