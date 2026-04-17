package br.com.investmentadvisor.application.service;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.model.TechnicalAnalysis;
import br.com.investmentadvisor.domain.model.TechnicalSignal;
import br.com.investmentadvisor.domain.port.out.QuoteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicalAnalysisServiceTest {

    @Mock
    private QuoteRepositoryPort quoteRepository;

    @InjectMocks
    private TechnicalAnalysisService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tickersConfig", "PETR4,VALE3");
    }

    // ── analyze ──────────────────────────────────────────────────────────────

    @Test
    void analyze_quandoNaoHaCotacoes_deveLancarException() {
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(List.of());

        assertThatThrownBy(() -> service.analyze("PETR4"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PETR4");
    }

    @Test
    void analyze_comUmaCotacao_deveRetornarAnaliseComSinaisNulos() {
        when(quoteRepository.findLatestByTicker("PETR4", 100))
                .thenReturn(List.of(quote("PETR4", 35.0)));

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.ticker()).isEqualTo("PETR4");
        assertThat(result.price()).isEqualByComparingTo("35.0");
        assertThat(result.sma20()).isNull();
        assertThat(result.sma50()).isNull();
        assertThat(result.rsi()).isNull();
        assertThat(result.signal()).isEqualTo(TechnicalSignal.HOLD);
    }

    // ── SMA ──────────────────────────────────────────────────────────────────

    @Test
    void analyze_com20Cotacoes_deveCalcularSma20() {
        List<Quote> quotes = quotesWithConstantPrice("PETR4", 10.0, 20);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.sma20()).isEqualByComparingTo("10.00");
        assertThat(result.sma50()).isNull();
    }

    @Test
    void analyze_com50Cotacoes_deveCalcularSma20eSma50() {
        List<Quote> quotes = quotesWithConstantPrice("PETR4", 20.0, 50);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.sma20()).isEqualByComparingTo("20.00");
        assertThat(result.sma50()).isEqualByComparingTo("20.00");
    }

    @Test
    void analyze_sma20_deveUsarApenasOsUltimos20Precos() {
        // Primeiros 30 com preço 5.0, últimos 20 com preço 10.0 → SMA20 = 10.0
        List<Quote> quotes = new ArrayList<>();
        quotes.addAll(quotesWithConstantPrice("PETR4", 5.0, 30));
        quotes.addAll(quotesWithConstantPrice("PETR4", 10.0, 20));
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.sma20()).isEqualByComparingTo("10.00");
    }

    // ── RSI ──────────────────────────────────────────────────────────────────

    @Test
    void analyze_comMenosDe15Cotacoes_rsiDeveSerNulo() {
        when(quoteRepository.findLatestByTicker("PETR4", 100))
                .thenReturn(quotesWithConstantPrice("PETR4", 10.0, 14));

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.rsi()).isNull();
    }

    @Test
    void analyze_com15CotacoesComTodoGanho_rsiDeveSer100() {
        // Preços sempre subindo → avgLoss = 0 → RSI = 100
        List<Quote> quotes = quotesWithIncreasingPrice("PETR4", 1.0, 15);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.rsi()).isEqualByComparingTo("100");
    }

    @Test
    void analyze_com15CotacoesComTodaPerda_rsiDeveSerZero() {
        // Preços sempre caindo → avgGain = 0 → RS = 0 → RSI = 0
        List<Quote> quotes = quotesWithDecreasingPrice("PETR4", 15.0, 15);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.rsi()).isEqualByComparingTo("0.00");
    }

    @Test
    void analyze_com15CotacoesComGanhosEPerdasIguais_rsiDeveSerCinquenta() {
        // Alternando +1 e -1 → ganhos = perdas → RS = 1 → RSI = 50
        List<Quote> quotes = quotesAlternating("PETR4", 10.0, 15);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.rsi()).isEqualByComparingTo("50.00");
    }

    // ── Sinais ───────────────────────────────────────────────────────────────

    @Test
    void analyze_rsiAbaixoDe30_deveGerarSinalBuy() {
        // Preços só caindo → RSI = 0 → BUY
        List<Quote> quotes = quotesWithDecreasingPrice("PETR4", 15.0, 15);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        assertThat(service.analyze("PETR4").signal()).isEqualTo(TechnicalSignal.BUY);
    }

    @Test
    void analyze_rsiAcimaDe70_deveGerarSinalSell() {
        // Preços só subindo → RSI = 100 → SELL
        List<Quote> quotes = quotesWithIncreasingPrice("PETR4", 1.0, 15);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        assertThat(service.analyze("PETR4").signal()).isEqualTo(TechnicalSignal.SELL);
    }

    @Test
    void analyze_tendenciaDeAlta_deveGerarSinalBuy() {
        // RSI ≈ 50, preço > SMA20 > SMA50 → BUY
        List<Quote> quotes = new ArrayList<>();
        quotes.addAll(quotesWithConstantPrice("PETR4", 10.0, 50)); // base para SMA50
        quotes.addAll(quotesAlternating("PETR4", 20.0, 15));       // RSI ≈ 50, preço alto
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        TechnicalAnalysis result = service.analyze("PETR4");

        assertThat(result.signal()).isEqualTo(TechnicalSignal.BUY);
    }

    @Test
    void analyze_semSmaSuficiente_sinaisNulosCausaHold() {
        // Apenas 15 quotes → RSI ≈ 50, sem SMA → HOLD
        List<Quote> quotes = quotesAlternating("PETR4", 10.0, 15);
        when(quoteRepository.findLatestByTicker("PETR4", 100)).thenReturn(quotes);

        assertThat(service.analyze("PETR4").signal()).isEqualTo(TechnicalSignal.HOLD);
    }

    // ── analyzeAll ───────────────────────────────────────────────────────────

    @Test
    void analyzeAll_deveAnalisarApenasTickersComCotacao() {
        when(quoteRepository.findLatest("PETR4"))
                .thenReturn(Optional.of(quote("PETR4", 35.0)));
        when(quoteRepository.findLatest("VALE3"))
                .thenReturn(Optional.empty());
        when(quoteRepository.findLatestByTicker("PETR4", 100))
                .thenReturn(List.of(quote("PETR4", 35.0)));

        List<TechnicalAnalysis> result = service.analyzeAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticker()).isEqualTo("PETR4");
    }

    @Test
    void analyzeAll_semNenhumDado_deveRetornarListaVazia() {
        when(quoteRepository.findLatest(any())).thenReturn(Optional.empty());

        assertThat(service.analyzeAll()).isEmpty();
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private Quote quote(String ticker, double price) {
        BigDecimal p = BigDecimal.valueOf(price);
        return new Quote(ticker, p, p, p, p, p, 1000L, LocalDateTime.now());
    }

    private List<Quote> quotesWithConstantPrice(String ticker, double price, int count) {
        List<Quote> quotes = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(count);
        BigDecimal p = BigDecimal.valueOf(price);
        for (int i = 0; i < count; i++) {
            quotes.add(new Quote(ticker, p, p, p, p, p, 1000L, base.plusDays(i)));
        }
        return quotes;
    }

    private List<Quote> quotesWithIncreasingPrice(String ticker, double start, int count) {
        List<Quote> quotes = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(count);
        for (int i = 0; i < count; i++) {
            BigDecimal p = BigDecimal.valueOf(start + i);
            quotes.add(new Quote(ticker, p, p, p, p, p, 1000L, base.plusDays(i)));
        }
        return quotes;
    }

    private List<Quote> quotesWithDecreasingPrice(String ticker, double start, int count) {
        List<Quote> quotes = new ArrayList<>();
        LocalDateTime base = LocalDateTime.now().minusDays(count);
        for (int i = 0; i < count; i++) {
            BigDecimal p = BigDecimal.valueOf(start - i);
            quotes.add(new Quote(ticker, p, p, p, p, p, 1000L, base.plusDays(i)));
        }
        return quotes;
    }

    /** Alternates +1 / -1 around a base price, producing RSI ≈ 50. */
    private List<Quote> quotesAlternating(String ticker, double base, int count) {
        List<Quote> quotes = new ArrayList<>();
        LocalDateTime timestamp = LocalDateTime.now().minusDays(count);
        for (int i = 0; i < count; i++) {
            double price = (i % 2 == 0) ? base : base + 1;
            BigDecimal p = BigDecimal.valueOf(price);
            quotes.add(new Quote(ticker, p, p, p, p, p, 1000L, timestamp.plusDays(i)));
        }
        return quotes;
    }
}