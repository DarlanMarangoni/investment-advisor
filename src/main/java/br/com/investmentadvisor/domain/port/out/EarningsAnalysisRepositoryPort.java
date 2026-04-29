package br.com.investmentadvisor.domain.port.out;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;

import java.util.List;
import java.util.Optional;

public interface EarningsAnalysisRepositoryPort {

    void save(EarningsAnalysis analysis);

    List<EarningsAnalysis> findAll();

    Optional<EarningsAnalysis> findByKey(String ticker, int referenceQuarter, int referenceYear);
}
