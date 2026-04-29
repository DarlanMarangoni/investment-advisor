package br.com.investmentadvisor.application.service;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import br.com.investmentadvisor.domain.port.in.GetEarningsAnalysesUseCase;
import br.com.investmentadvisor.domain.port.in.UploadEarningsReportUseCase;
import br.com.investmentadvisor.domain.port.out.EarningsAnalysisPort;
import br.com.investmentadvisor.domain.port.out.EarningsAnalysisRepositoryPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EarningsReportService implements UploadEarningsReportUseCase, GetEarningsAnalysesUseCase {

    private final EarningsAnalysisPort earningsAnalysisPort;
    private final EarningsAnalysisRepositoryPort earningsAnalysisRepositoryPort;

    public EarningsReportService(EarningsAnalysisPort earningsAnalysisPort,
                                 EarningsAnalysisRepositoryPort earningsAnalysisRepositoryPort) {
        this.earningsAnalysisPort = earningsAnalysisPort;
        this.earningsAnalysisRepositoryPort = earningsAnalysisRepositoryPort;
    }

    @Override
    public EarningsAnalysis upload(byte[] pdfBytes, String ticker, int referenceQuarter, int referenceYear) {
        validatePdf(pdfBytes);
        String normalizedTicker = ticker.toUpperCase();

        return earningsAnalysisRepositoryPort
                .findByKey(normalizedTicker, referenceQuarter, referenceYear)
                .orElseGet(() -> analyze(pdfBytes, normalizedTicker, referenceQuarter, referenceYear));
    }

    private EarningsAnalysis analyze(byte[] pdfBytes, String ticker, int referenceQuarter, int referenceYear) {
        String reportText = extractText(pdfBytes);
        String analysis = earningsAnalysisPort.analyze(reportText, ticker);
        EarningsAnalysis result = new EarningsAnalysis(ticker, referenceQuarter, referenceYear, analysis, LocalDateTime.now());
        earningsAnalysisRepositoryPort.save(result);
        return result;
    }

    @Override
    public List<EarningsAnalysis> findAll() {
        return earningsAnalysisRepositoryPort.findAll();
    }

    private void validatePdf(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Arquivo não pode estar vazio.");
        }
        // Valida magic bytes do PDF: %PDF (0x25 0x50 0x44 0x46)
        if (bytes.length < 4 || bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
            throw new IllegalArgumentException("Apenas arquivos PDF são aceitos.");
        }
    }

    private String extractText(byte[] bytes) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao processar o PDF: " + e.getMessage(), e);
        }
    }
}
