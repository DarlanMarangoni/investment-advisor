package br.com.investmentadvisor.domain.port.out;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;

import java.util.List;

public interface EarningsAnalysisRepositoryPort {

    void save(EarningsAnalysis analysis);

    List<EarningsAnalysis> findAll();
}
