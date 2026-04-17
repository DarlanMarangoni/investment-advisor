package br.com.investmentadvisor.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${app.brapi.base-url}")
    private String brapiBaseUrl;

    @Bean
    public RestClient brapiRestClient() {
        return RestClient.builder()
                .baseUrl(brapiBaseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}