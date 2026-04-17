package br.com.investmentadvisor.infrastructure.adapter.in.web;

import br.com.investmentadvisor.domain.model.TechnicalAnalysis;
import br.com.investmentadvisor.domain.port.in.FetchQuotesUseCase;
import br.com.investmentadvisor.domain.port.in.GetTechnicalAnalysisUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Investment Advisor", description = "Análise técnica de ações da B3")
public class QuoteController {

    private final GetTechnicalAnalysisUseCase getTechnicalAnalysisUseCase;
    private final FetchQuotesUseCase fetchQuotesUseCase;

    @GetMapping("/analysis")
    @Operation(summary = "Análise técnica de todos os ativos configurados")
    public ResponseEntity<List<TechnicalAnalysis>> analyzeAll() {
        return ResponseEntity.ok(getTechnicalAnalysisUseCase.analyzeAll());
    }

    @GetMapping("/analysis/{ticker}")
    @Operation(summary = "Análise técnica de um ativo específico")
    public ResponseEntity<TechnicalAnalysis> analyze(@PathVariable String ticker) {
        return ResponseEntity.ok(getTechnicalAnalysisUseCase.analyze(ticker.toUpperCase()));
    }

    @PostMapping("/quotes/fetch")
    @Operation(summary = "Dispara busca de cotações manualmente (fora do agendamento)")
    public ResponseEntity<Void> fetchNow() {
        fetchQuotesUseCase.fetchAndStoreQuotes();
        return ResponseEntity.accepted().build();
    }
}