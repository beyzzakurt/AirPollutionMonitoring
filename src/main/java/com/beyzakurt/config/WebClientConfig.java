package com.beyzakurt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(ExternalApiProperties externalApiProperties) {
        return WebClient.builder().baseUrl(("https://api.waqi.info")).build();
    }
}
