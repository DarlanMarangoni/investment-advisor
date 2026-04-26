package br.com.investmentadvisor.infrastructure.adapter.in.web;

import br.com.investmentadvisor.domain.port.in.UploadEarningsReportUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/earnings")
@RequiredArgsConstructor
@Tag(name = "Earnings Reports", description = "Análise de divulgação de resultados")
public class EarningsReportController {

    private final UploadEarningsReportUseCase uploadEarningsReportUseCase;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload de um PDF de resultado e extrai o texto")
    public ResponseEntity<String> upload(@RequestPart("file") MultipartFile file) {
        String extractedText = uploadEarningsReportUseCase.upload(file);
        return ResponseEntity.ok(extractedText);
    }
}
