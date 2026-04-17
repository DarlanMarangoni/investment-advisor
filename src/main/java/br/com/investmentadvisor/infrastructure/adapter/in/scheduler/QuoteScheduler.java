package br.com.investmentadvisor.infrastructure.adapter.in.scheduler;

import br.com.investmentadvisor.domain.port.in.FetchQuotesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuoteScheduler {

    private final FetchQuotesUseCase fetchQuotesUseCase;

    /**
     * Executa de segunda a sexta, das 10h às 17h45, a cada 15 minutos.
     * Cron configurável via app.scheduler.cron no application.yml.
     */
    @Scheduled(cron = "${app.scheduler.cron}")
    public void scheduledFetch() {
        log.info("[{}] Scheduler disparado — buscando cotações...", LocalDateTime.now());
        fetchQuotesUseCase.fetchAndStoreQuotes();
    }
}