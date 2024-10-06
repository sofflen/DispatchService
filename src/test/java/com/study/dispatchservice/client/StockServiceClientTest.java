package com.study.dispatchservice.client;

import com.study.dispatchservice.exceptions.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceClientTest {
    private static final String ITEM_NAME = "my-item";
    private static final String STOCK_SERVICE_ENDPOINT = "endpoint";
    private static final String STOCK_SERVICE_QUERY = STOCK_SERVICE_ENDPOINT + "?item=" + ITEM_NAME;

    private RestTemplate restTemplateMock;
    private RestTemplateBuilder restTemplateBuilderMock;
    private StockServiceClient client;

    @BeforeEach
    void setUp() {
        restTemplateMock = mock(RestTemplate.class);
        restTemplateBuilderMock = mock(RestTemplateBuilder.class);

        when(restTemplateBuilderMock.build()).thenReturn(restTemplateMock);

        client = new StockServiceClient(restTemplateBuilderMock, STOCK_SERVICE_ENDPOINT);
    }

    @Test
    void testCheckAvailability_Success() {
        var response = new ResponseEntity<>("true", HttpStatus.OK);
        when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenReturn(response);

        assertThat(client.checkAvailability(ITEM_NAME), equalTo("true"));
        verify(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    void testCheckAvailability_ServerError() {
        doThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);

        assertThrows(RetryableException.class, () -> client.checkAvailability(ITEM_NAME));
        verify(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    void testCheckAvailability_ResourceAccessException() {
        doThrow(new ResourceAccessException("Access Exception"))
                .when(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);

        assertThrows(RetryableException.class, () -> client.checkAvailability(ITEM_NAME));
        verify(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }

    @Test
    void testCheckAvailability_RuntimeException() {
        var response = new ResponseEntity<>("false", HttpStatus.BAD_REQUEST);
        when(restTemplateMock.getForEntity(STOCK_SERVICE_QUERY, String.class)).thenReturn(response);

        assertThrows(IllegalArgumentException.class, () -> client.checkAvailability(ITEM_NAME));
        verify(restTemplateMock).getForEntity(STOCK_SERVICE_QUERY, String.class);
    }
}