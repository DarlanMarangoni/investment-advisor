package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "earnings_analysis", indexes = {
        @Index(name = "idx_earnings_ticker", columnList = "ticker"),
        @Index(name = "idx_earnings_analyzed_at", columnList = "analyzed_at DESC")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EarningsAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String analysis;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;
}
