package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import br.com.investmentadvisor.domain.port.out.EarningsAnalysisRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostgresEarningsAnalysisRepository implements EarningsAnalysisRepositoryPort {

    private final EarningsAnalysisJpaRepository jpaRepository;

    @Override
    @Transactional("analyticsTransactionManager")
    public void save(EarningsAnalysis analysis) {
        jpaRepository.save(EarningsAnalysisEntity.builder()
                .id(new EarningsAnalysisId(analysis.ticker(), analysis.referenceQuarter(), analysis.referenceYear()))
                .analysis(analysis.analysis())
                .analyzedAt(analysis.analyzedAt())
                .build());
    }

    @Override
    @Transactional(value = "analyticsTransactionManager", readOnly = true)
    public List<EarningsAnalysis> findAll() {
        return jpaRepository.findAllByOrderByAnalyzedAtDesc().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional(value = "analyticsTransactionManager", readOnly = true)
    public Optional<EarningsAnalysis> findByKey(String ticker, int referenceQuarter, int referenceYear) {
        return jpaRepository.findById(new EarningsAnalysisId(ticker, referenceQuarter, referenceYear))
                .map(this::toModel);
    }

    private EarningsAnalysis toModel(EarningsAnalysisEntity e) {
        return new EarningsAnalysis(
                e.getId().getTicker(),
                e.getId().getReferenceQuarter(),
                e.getId().getReferenceYear(),
                e.getAnalysis(),
                e.getAnalyzedAt());
    }
}
