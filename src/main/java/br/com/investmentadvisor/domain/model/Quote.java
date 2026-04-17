package br.com.investmentadvisor.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Quote(
        String ticker,
        BigDecimal price,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume,
        LocalDateTime timestamp
) {}