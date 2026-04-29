package br.com.investmentadvisor.infrastructure.adapter.out.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EarningsAnalysisId implements Serializable {

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(name = "reference_quarter", nullable = false)
    private Integer referenceQuarter;

    @Column(name = "reference_year", nullable = false)
    private Integer referenceYear;
}
