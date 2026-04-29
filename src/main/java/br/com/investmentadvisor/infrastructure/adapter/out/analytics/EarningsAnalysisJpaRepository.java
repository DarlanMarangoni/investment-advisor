package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EarningsAnalysisJpaRepository extends JpaRepository<EarningsAnalysisEntity, EarningsAnalysisId> {

    List<EarningsAnalysisEntity> findAllByOrderByAnalyzedAtDesc();

    Optional<EarningsAnalysisEntity> findById(EarningsAnalysisId id);
}
