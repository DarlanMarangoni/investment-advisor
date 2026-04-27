package br.com.investmentadvisor.infrastructure.adapter.in.web;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import br.com.investmentadvisor.domain.port.in.GetEarningsAnalysesUseCase;
import br.com.investmentadvisor.domain.port.in.UploadEarningsReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/earnings")
@RequiredArgsConstructor
@Tag(name = "Earnings Reports", description = "Análise de divulgação de resultados com IA")
public class EarningsReportController {

    private final UploadEarningsReportUseCase uploadEarningsReportUseCase;
    private final GetEarningsAnalysesUseCase getEarningsAnalysesUseCase;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload de um PDF de resultado e retorna análise gerada por IA")
    public ResponseEntity<EarningsAnalysis> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("ticker") String ticker) throws IOException {
        return ResponseEntity.ok(uploadEarningsReportUseCase.upload(file.getBytes(), ticker));
    }

    @GetMapping
    @Operation(summary = "Lista todas as análises de resultados salvas")
    public ResponseEntity<List<EarningsAnalysis>> findAll() {
        return ResponseEntity.ok(getEarningsAnalysesUseCase.findAll());
    }
}
