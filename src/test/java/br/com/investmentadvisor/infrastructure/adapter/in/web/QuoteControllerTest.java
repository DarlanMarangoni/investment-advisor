package br.com.investmentadvisor.infrastructure.adapter.in.web;

import br.com.investmentadvisor.domain.model.TechnicalAnalysis;
import br.com.investmentadvisor.domain.model.TechnicalSignal;
import br.com.investmentadvisor.domain.port.in.FetchQuotesUseCase;
import br.com.investmentadvisor.domain.port.in.GetTechnicalAnalysisUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteControllerTest {

    @Mock
    private GetTechnicalAnalysisUseCase getTechnicalAnalysisUseCase;

    @Mock
    private FetchQuotesUseCase fetchQuotesUseCase;

    @InjectMocks
    private QuoteController controller;

    @Test
    void analyzeAll_deveRetornar200ComListaDeAnalises() {
        when(getTechnicalAnalysisUseCase.analyzeAll()).thenReturn(List.of(analysis("PETR4")));

        ResponseEntity<List<TechnicalAnalysis>> response = controller.analyzeAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).ticker()).isEqualTo("PETR4");
    }

    @Test
    void analyzeAll_quandoVazio_deveRetornar200ComListaVazia() {
        when(getTechnicalAnalysisUseCase.analyzeAll()).thenReturn(List.of());

        ResponseEntity<List<TechnicalAnalysis>> response = controller.analyzeAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void analyze_deveRetornar200ComAnalise() {
        when(getTechnicalAnalysisUseCase.analyze("PETR4")).thenReturn(analysis("PETR4"));

        ResponseEntity<TechnicalAnalysis> response = controller.analyze("PETR4");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().ticker()).isEqualTo("PETR4");
    }

    @Test
    void analyze_deveConverterTickerParaMaiusculo() {
        when(getTechnicalAnalysisUseCase.analyze("PETR4")).thenReturn(analysis("PETR4"));

        controller.analyze("petr4");

        verify(getTechnicalAnalysisUseCase).analyze("PETR4");
    }

    @Test
    void analyze_tickerSemCotacao_devePropagrarException() {
        when(getTechnicalAnalysisUseCase.analyze("XXXX"))
                .thenThrow(new IllegalArgumentException("Nenhuma cotação encontrada para o ticker: XXXX"));

        assertThatThrownBy(() -> controller.analyze("XXXX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XXXX");
    }

    @Test
    void fetchNow_deveRetornar202EDisparaFetch() {
        ResponseEntity<Void> response = controller.fetchNow();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(fetchQuotesUseCase).fetchAndStoreQuotes();
    }

    private TechnicalAnalysis analysis(String ticker) {
        return new TechnicalAnalysis(
                ticker,
                BigDecimal.valueOf(35.50),
                BigDecimal.valueOf(34.00),
                BigDecimal.valueOf(33.00),
                BigDecimal.valueOf(25.00),
                TechnicalSignal.BUY,
                LocalDateTime.now()
        );
    }
}