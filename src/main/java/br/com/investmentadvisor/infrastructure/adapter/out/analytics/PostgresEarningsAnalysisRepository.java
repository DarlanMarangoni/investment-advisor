package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import br.com.investmentadvisor.domain.port.out.EarningsAnalysisRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostgresEarningsAnalysisRepository implements EarningsAnalysisRepositoryPort {

    private final EarningsAnalysisJpaRepository jpaRepository;

    @Override
    @Transactional("analyticsTransactionManager")
    public void save(EarningsAnalysis analysis) {
        jpaRepository.save(EarningsAnalysisEntity.builder()
                .ticker(analysis.ticker())
                .analysis(analysis.analysis())
                .analyzedAt(analysis.analyzedAt())
                .build());
    }

    @Override
    @Transactional(value = "analyticsTransactionManager", readOnly = true)
    public List<EarningsAnalysis> findAll() {
        return jpaRepository.findAllByOrderByAnalyzedAtDesc().stream()
                .map(e -> new EarningsAnalysis(e.getTicker(), e.getAnalysis(), e.getAnalyzedAt()))
                .toList();
    }
}
