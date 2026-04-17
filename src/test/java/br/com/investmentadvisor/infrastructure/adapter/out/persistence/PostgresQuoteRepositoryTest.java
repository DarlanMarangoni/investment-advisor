package br.com.investmentadvisor.infrastructure.adapter.out.persistence;

import br.com.investmentadvisor.domain.model.Quote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresQuoteRepositoryTest {

    @Mock
    private QuoteJpaRepository jpaRepository;

    @InjectMocks
    private PostgresQuoteRepository repository;

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_deveMappearDomainParaEntityCorretamente() {
        LocalDateTime now = LocalDateTime.now();
        Quote quote = new Quote("PETR4", bd(35.50), bd(34.80), bd(36.00), bd(34.50), bd(35.50), 5000000L, now);

        repository.save(quote);

        ArgumentCaptor<QuoteEntity> captor = ArgumentCaptor.forClass(QuoteEntity.class);
        verify(jpaRepository).save(captor.capture());

        QuoteEntity saved = captor.getValue();
        assertThat(saved.getTicker()).isEqualTo("PETR4");
        assertThat(saved.getPrice()).isEqualByComparingTo("35.50");
        assertThat(saved.getOpen()).isEqualByComparingTo("34.80");
        assertThat(saved.getHigh()).isEqualByComparingTo("36.00");
        assertThat(saved.getLow()).isEqualByComparingTo("34.50");
        assertThat(saved.getClose()).isEqualByComparingTo("35.50");
        assertThat(saved.getVolume()).isEqualTo(5000000L);
        assertThat(saved.getTimestamp()).isEqualTo(now);
    }

    // ── findLatestByTicker ────────────────────────────────────────────────────

    @Test
    void findLatestByTicker_deveRetornarEmOrdemCronologica() {
        // JPA retorna DESC (mais recente primeiro), o repository deve inverter para ASC
        LocalDateTime t1 = LocalDateTime.now().minusMinutes(30);
        LocalDateTime t2 = LocalDateTime.now().minusMinutes(15);
        LocalDateTime t3 = LocalDateTime.now();

        QuoteEntity entity1 = entity("PETR4", 35.0, t3); // mais recente
        QuoteEntity entity2 = entity("PETR4", 34.5, t2);
        QuoteEntity entity3 = entity("PETR4", 34.0, t1); // mais antigo

        when(jpaRepository.findLatestByTicker("PETR4", PageRequest.of(0, 3)))
                .thenReturn(List.of(entity1, entity2, entity3));

        List<Quote> result = repository.findLatestByTicker("PETR4", 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).timestamp()).isEqualTo(t1); // mais antigo primeiro
        assertThat(result.get(1).timestamp()).isEqualTo(t2);
        assertThat(result.get(2).timestamp()).isEqualTo(t3); // mais recente por último
    }

    @Test
    void findLatestByTicker_devePassarLimitCorretoAoJpa() {
        when(jpaRepository.findLatestByTicker("PETR4", PageRequest.of(0, 50)))
                .thenReturn(List.of());

        repository.findLatestByTicker("PETR4", 50);

        verify(jpaRepository).findLatestByTicker("PETR4", PageRequest.of(0, 50));
    }

    // ── findByTicker ──────────────────────────────────────────────────────────

    @Test
    void findByTicker_deveMappearEntitiesParaDomain() {
        LocalDateTime now = LocalDateTime.now();
        when(jpaRepository.findAllByTickerOrderByTimestampAsc("PETR4"))
                .thenReturn(List.of(entity("PETR4", 35.0, now)));

        List<Quote> result = repository.findByTicker("PETR4");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ticker()).isEqualTo("PETR4");
        assertThat(result.get(0).price()).isEqualByComparingTo("35.0");
    }

    // ── findLatest ────────────────────────────────────────────────────────────

    @Test
    void findLatest_quandoExiste_deveRetornarQuote() {
        LocalDateTime now = LocalDateTime.now();
        when(jpaRepository.findTopByTickerOrderByTimestampDesc("PETR4"))
                .thenReturn(Optional.of(entity("PETR4", 35.0, now)));

        Optional<Quote> result = repository.findLatest("PETR4");

        assertThat(result).isPresent();
        assertThat(result.get().ticker()).isEqualTo("PETR4");
    }

    @Test
    void findLatest_quandoNaoExiste_deveRetornarEmpty() {
        when(jpaRepository.findTopByTickerOrderByTimestampDesc("PETR4"))
                .thenReturn(Optional.empty());

        assertThat(repository.findLatest("PETR4")).isEmpty();
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private BigDecimal bd(double value) {
        return BigDecimal.valueOf(value);
    }

    private QuoteEntity entity(String ticker, double price, LocalDateTime timestamp) {
        return QuoteEntity.builder()
                .id(1L)
                .ticker(ticker)
                .price(bd(price))
                .open(bd(price))
                .high(bd(price))
                .low(bd(price))
                .close(bd(price))
                .volume(1000L)
                .timestamp(timestamp)
                .build();
    }
}