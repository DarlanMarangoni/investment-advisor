package br.com.investmentadvisor.domain.port.out;

public interface EarningsAnalysisPort {

    String analyze(String reportText, String ticker);
}
