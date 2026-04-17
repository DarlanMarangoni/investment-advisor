package br.com.investmentadvisor.infrastructure.adapter.out.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiResponse(List<BrapiResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiResult(
            String symbol,
            String shortName,
            String longName,
            String currency,
            BigDecimal regularMarketPrice,
            BigDecimal regularMarketOpen,
            BigDecimal regularMarketDayHigh,
            BigDecimal regularMarketDayLow,
            BigDecimal regularMarketPreviousClose,
            Long regularMarketVolume
    ) {}
}