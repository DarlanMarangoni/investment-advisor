package br.com.investmentadvisor.domain.port.in;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;

import java.util.List;

public interface GetEarningsAnalysesUseCase {

    List<EarningsAnalysis> findAll();
}
