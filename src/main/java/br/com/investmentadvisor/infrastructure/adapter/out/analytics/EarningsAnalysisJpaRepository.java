package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EarningsAnalysisJpaRepository extends JpaRepository<EarningsAnalysisEntity, Long> {

    List<EarningsAnalysisEntity> findAllByOrderByAnalyzedAtDesc();
}
