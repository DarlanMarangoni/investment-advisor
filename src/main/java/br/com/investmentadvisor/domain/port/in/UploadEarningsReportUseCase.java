package br.com.investmentadvisor.domain.port.in;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;

public interface UploadEarningsReportUseCase {

    EarningsAnalysis upload(byte[] pdfBytes, String ticker, int referenceQuarter, int referenceYear);
}
