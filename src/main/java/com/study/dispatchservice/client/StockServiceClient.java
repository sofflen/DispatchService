package com.study.dispatchservice.client;

import com.study.dispatchservice.exceptions.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class StockServiceClient {

    private final RestTemplate restTemplate;
    private final String stockServiceEndpoint;

    public StockServiceClient(RestTemplateBuilder restTemplateBuilder,
                              @Value("${dispatch.stock-service.endpoint}") String stockServiceEndpoint) {
        this.restTemplate = restTemplateBuilder.build();
        this.stockServiceEndpoint = stockServiceEndpoint;
    }

    public String checkAvailability(String item) {
        try {
            var response = restTemplate.getForEntity(stockServiceEndpoint+"?item="+item, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Stock service returned an unsuccessful response: " + response.getBody());
            }
            return response.getBody();
        } catch (HttpServerErrorException | ResourceAccessException e) {
            log.warn("Failure calling external service", e);
            throw new RetryableException(e);
        } catch (Exception e) {
            log.error("Exception thrown", e);
            throw e;
        }
    }
}
