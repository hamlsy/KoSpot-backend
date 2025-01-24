package com.kospot.kospot.global.config.webClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private static final String BASE_URL = "http://localhost:8080";

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(BASE_URL) // 기본 URL 설정 (필요시 변경)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
