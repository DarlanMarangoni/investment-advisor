package br.com.investmentadvisor.domain.port.in;

import org.springframework.web.multipart.MultipartFile;

public interface UploadEarningsReportUseCase {

    String upload(MultipartFile file);
}
