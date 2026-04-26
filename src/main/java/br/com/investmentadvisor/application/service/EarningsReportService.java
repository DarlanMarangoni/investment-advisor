package br.com.investmentadvisor.application.service;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import br.com.investmentadvisor.domain.port.in.UploadEarningsReportUseCase;
import br.com.investmentadvisor.domain.port.out.EarningsAnalysisPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EarningsReportService implements UploadEarningsReportUseCase {

    private final EarningsAnalysisPort earningsAnalysisPort;

    public EarningsReportService(EarningsAnalysisPort earningsAnalysisPort) {
        this.earningsAnalysisPort = earningsAnalysisPort;
    }

    @Override
    public EarningsAnalysis upload(MultipartFile file, String ticker) {
        validatePdf(file);
        String reportText = extractText(file);
        String analysis = earningsAnalysisPort.analyze(reportText, ticker.toUpperCase());
        return new EarningsAnalysis(ticker.toUpperCase(), analysis);
    }

    private void validatePdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode estar vazio.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Apenas arquivos PDF são aceitos.");
        }
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao processar o PDF: " + e.getMessage(), e);
        }
    }
}
