package br.com.investmentadvisor.application.service;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.port.out.QuoteRepositoryPort;
import br.com.investmentadvisor.domain.port.out.StockQuotePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchQuotesServiceTest {

    @Mock
    private StockQuotePort stockQuotePort;

    @Mock
    private QuoteRepositoryPort quoteRepository;

    @InjectMocks
    private FetchQuotesService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tickersConfig", "PETR4,VALE3");
    }

    @Test
    void fetchAndStoreQuotes_deveBuscarCotacoesParaTodosOsTickers() {
        when(stockQuotePort.fetchQuotes(List.of("PETR4", "VALE3"))).thenReturn(List.of());

        service.fetchAndStoreQuotes();

        verify(stockQuotePort).fetchQuotes(List.of("PETR4", "VALE3"));
    }

    @Test
    void fetchAndStoreQuotes_deveSalvarCadaCotacaoRetornada() {
        Quote petr4 = quote("PETR4", 35.50);
        Quote vale3 = quote("VALE3", 68.20);
        when(stockQuotePort.fetchQuotes(anyList())).thenReturn(List.of(petr4, vale3));

        service.fetchAndStoreQuotes();

        verify(quoteRepository).save(petr4);
        verify(quoteRepository).save(vale3);
    }

    @Test
    void fetchAndStoreQuotes_quandoApiRetornaVazio_naoDeveSalvarNada() {
        when(stockQuotePort.fetchQuotes(anyList())).thenReturn(List.of());

        service.fetchAndStoreQuotes();

        verify(quoteRepository, never()).save(any());
    }

    private Quote quote(String ticker, double price) {
        BigDecimal p = BigDecimal.valueOf(price);
        return new Quote(ticker, p, p, p, p, p, 1000L, LocalDateTime.now());
    }
}