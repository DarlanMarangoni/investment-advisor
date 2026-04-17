package br.com.investmentadvisor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvestmentAdvisorApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentAdvisorApplication.class, args);
    }
}