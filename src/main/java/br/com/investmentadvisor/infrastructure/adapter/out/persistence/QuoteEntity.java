package br.com.investmentadvisor.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes", indexes = {
        @Index(name = "idx_quotes_ticker_timestamp", columnList = "ticker, timestamp DESC")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String ticker;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(precision = 15, scale = 4)
    private BigDecimal open;

    @Column(precision = 15, scale = 4)
    private BigDecimal high;

    @Column(precision = 15, scale = 4)
    private BigDecimal low;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal close;

    private Long volume;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}