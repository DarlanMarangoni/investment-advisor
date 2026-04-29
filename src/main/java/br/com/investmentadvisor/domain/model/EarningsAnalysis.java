package br.com.investmentadvisor.domain.model;

import java.time.LocalDateTime;

public record EarningsAnalysis(String ticker, int referenceQuarter, int referenceYear, String analysis, LocalDateTime analyzedAt) {}
