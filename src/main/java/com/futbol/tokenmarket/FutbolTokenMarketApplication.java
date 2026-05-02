package com.futbol.tokenmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class FutbolTokenMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(FutbolTokenMarketApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        
        // Agregar interceptor para el header de autorización
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("X-Auth-Token", "82d59be660724438a0020cfd739c1413");
            return execution.execute(request, body);
        });
        
        return restTemplate;
    }
}
