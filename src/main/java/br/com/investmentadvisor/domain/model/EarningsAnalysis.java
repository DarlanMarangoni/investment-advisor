package br.com.investmentadvisor.domain.model;

import java.time.LocalDateTime;

public record EarningsAnalysis(String ticker, String analysis, LocalDateTime analyzedAt) {}
