package br.com.investmentadvisor.application.service;

import br.com.investmentadvisor.domain.model.Quote;
import br.com.investmentadvisor.domain.model.TechnicalAnalysis;
import br.com.investmentadvisor.domain.model.TechnicalSignal;
import br.com.investmentadvisor.domain.port.in.GetTechnicalAnalysisUseCase;
import br.com.investmentadvisor.domain.port.out.QuoteRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalAnalysisService implements GetTechnicalAnalysisUseCase {

    private final QuoteRepositoryPort quoteRepository;

    @Value("${app.stocks.tickers}")
    private String tickersConfig;

    @Override
    public TechnicalAnalysis analyze(String ticker) {
        List<Quote> quotes = quoteRepository.findLatestByTicker(ticker, 100);
        if (quotes.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma cotação encontrada para o ticker: " + ticker);
        }
        return buildAnalysis(ticker, quotes);
    }

    @Override
    public List<TechnicalAnalysis> analyzeAll() {
        return Arrays.stream(tickersConfig.split(","))
                .filter(ticker -> quoteRepository.findLatest(ticker).isPresent())
                .map(this::analyze)
                .toList();
    }

    private TechnicalAnalysis buildAnalysis(String ticker, List<Quote> quotes) {
        Quote latest = quotes.getLast();
        BigDecimal sma20 = calculateSma(quotes, 20);
        BigDecimal sma50 = calculateSma(quotes, 50);
        BigDecimal rsi = calculateRsi(quotes);
        TechnicalSignal signal = generateSignal(rsi, sma20, sma50, latest.close());

        log.debug("{} | Preço: {} | SMA20: {} | SMA50: {} | RSI: {} | Sinal: {}",
                ticker, latest.price(), sma20, sma50, rsi, signal);

        return new TechnicalAnalysis(ticker, latest.price(), sma20, sma50, rsi, signal, LocalDateTime.now());
    }

    /**
     * Simple Moving Average (SMA) para N períodos usando o preço de fechamento.
     */
    private BigDecimal calculateSma(List<Quote> quotes, int periods) {
        if (quotes.size() < periods) return null;

        List<Quote> window = quotes.subList(quotes.size() - periods, quotes.size());
        BigDecimal sum = window.stream()
                .map(Quote::close)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
    }

    /**
     * Relative Strength Index (RSI) de 14 períodos.
     * RSI = 100 - (100 / (1 + RS))  onde RS = Média de ganhos / Média de perdas
     */
    private BigDecimal calculateRsi(List<Quote> quotes) {
        int periods = 14;
        if (quotes.size() < periods + 1) return null;

        List<Quote> window = quotes.subList(quotes.size() - (periods + 1), quotes.size());

        double totalGain = 0;
        double totalLoss = 0;

        for (int i = 1; i < window.size(); i++) {
            double change = window.get(i).close()
                    .subtract(window.get(i - 1).close())
                    .doubleValue();

            if (change > 0) totalGain += change;
            else totalLoss += Math.abs(change);
        }

        double avgGain = totalGain / periods;
        double avgLoss = totalLoss / periods;

        if (avgLoss == 0) return BigDecimal.valueOf(100);

        double rs = avgGain / avgLoss;
        double rsi = 100 - (100.0 / (1 + rs));

        return BigDecimal.valueOf(rsi).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Combina RSI com cruzamento de médias para gerar o sinal.
     * - RSI < 30: sobrevendido → BUY
     * - RSI > 70: sobrecomprado → SELL
     * - Preço > SMA20 > SMA50 e RSI < 60: tendência de alta → BUY
     * - Preço < SMA20 < SMA50 e RSI > 40: tendência de baixa → SELL
     */
    private TechnicalSignal generateSignal(BigDecimal rsi, BigDecimal sma20, BigDecimal sma50, BigDecimal price) {
        if (rsi == null) return TechnicalSignal.HOLD;

        double rsiValue = rsi.doubleValue();

        if (rsiValue < 30) return TechnicalSignal.BUY;
        if (rsiValue > 70) return TechnicalSignal.SELL;

        if (sma20 != null && sma50 != null) {
            boolean uptrend = price.compareTo(sma20) > 0 && sma20.compareTo(sma50) > 0;
            boolean downtrend = price.compareTo(sma20) < 0 && sma20.compareTo(sma50) < 0;

            if (uptrend && rsiValue < 60) return TechnicalSignal.BUY;
            if (downtrend && rsiValue > 40) return TechnicalSignal.SELL;
        }

        return TechnicalSignal.HOLD;
    }
}