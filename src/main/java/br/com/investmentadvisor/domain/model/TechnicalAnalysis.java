package br.com.investmentadvisor.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TechnicalAnalysis(
        String ticker,
        BigDecimal price,
        BigDecimal sma20,
        BigDecimal sma50,
        BigDecimal rsi,
        TechnicalSignal signal,
        LocalDateTime analyzedAt
) {}