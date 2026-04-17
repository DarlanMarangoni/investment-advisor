package br.com.investmentadvisor.domain.port.in;

import br.com.investmentadvisor.domain.model.TechnicalAnalysis;

import java.util.List;

public interface GetTechnicalAnalysisUseCase {
    TechnicalAnalysis analyze(String ticker);
    List<TechnicalAnalysis> analyzeAll();
}