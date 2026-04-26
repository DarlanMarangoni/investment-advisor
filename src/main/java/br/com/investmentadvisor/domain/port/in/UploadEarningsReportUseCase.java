package br.com.investmentadvisor.domain.port.in;

import br.com.investmentadvisor.domain.model.EarningsAnalysis;
import org.springframework.web.multipart.MultipartFile;

public interface UploadEarningsReportUseCase {

    EarningsAnalysis upload(MultipartFile file, String ticker);
}
