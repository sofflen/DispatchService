package com.study.dispatchservice.services;

import com.study.dispatchservice.client.StockServiceClient;
import com.study.dispatchservice.messages.DispatchCompletedEvent;
import com.study.dispatchservice.messages.DispatchPreparingEvent;
import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import com.study.dispatchservice.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.study.dispatchservice.services.DispatchService.DISPATCH_TRACKING_TOPIC;
import static com.study.dispatchservice.services.DispatchService.ORDER_DISPATCHED_TOPIC;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DispatchServiceTest {

    private DispatchService dispatchService;
    private KafkaTemplate<String, Object> kafkaTemplateMock;
    private StockServiceClient clientMock;
    private String testKey;
    private OrderCreatedEvent testOrderCreatedEvent;

    @BeforeEach
    void setUp() {
        kafkaTemplateMock = mock(KafkaTemplate.class);
        clientMock = mock(StockServiceClient.class);
        dispatchService = new DispatchService(kafkaTemplateMock, clientMock);
        testKey = UUID.randomUUID().toString();
        testOrderCreatedEvent = EventUtils.randomOrderCreatedEvent();
    }

    @Test
    void process_success() throws Exception {
        when(clientMock.checkAvailability(anyString())).thenReturn(Boolean.TRUE.toString());
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparingEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchCompletedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));

        dispatchService.process(testKey, testOrderCreatedEvent);

        verify(clientMock).checkAvailability(testOrderCreatedEvent.getItem());
        verify(kafkaTemplateMock).send(eq(ORDER_DISPATCHED_TOPIC), eq(testKey), any(OrderDispatchedEvent.class));
        verify(kafkaTemplateMock).send(eq(DISPATCH_TRACKING_TOPIC), eq(testKey), any(DispatchPreparingEvent.class));
        verify(kafkaTemplateMock).send(eq(DISPATCH_TRACKING_TOPIC), eq(testKey), any(DispatchCompletedEvent.class));
    }

    @Test
    void process_itemNotAvailable() throws Exception {
        when(clientMock.checkAvailability(anyString())).thenReturn(Boolean.FALSE.toString());

        dispatchService.process(testKey, testOrderCreatedEvent);

        verify(clientMock).checkAvailability(testOrderCreatedEvent.getItem());
        verifyNoInteractions(kafkaTemplateMock);
    }

    @Test
    void process_throwsExceptionOnOrderDispatchedEvent() {
        when(clientMock.checkAvailability(anyString())).thenReturn(Boolean.TRUE.toString());
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        doThrow(new RuntimeException()).when(kafkaTemplateMock).send(anyString(), anyString(), any(OrderDispatchedEvent.class));

        assertThrows(RuntimeException.class, () -> dispatchService.process(testKey, testOrderCreatedEvent));
        verify(clientMock).checkAvailability(testOrderCreatedEvent.getItem());
        verify(kafkaTemplateMock).send(eq(ORDER_DISPATCHED_TOPIC), eq(testKey), any(OrderDispatchedEvent.class));
        verifyNoMoreInteractions(kafkaTemplateMock);
    }

    @Test
    void process_throwsExceptionOnDispatchPreparingEvent() {
        when(clientMock.checkAvailability(anyString())).thenReturn(Boolean.TRUE.toString());
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparingEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        doThrow(new RuntimeException()).when(kafkaTemplateMock).send(anyString(), anyString(), any(DispatchPreparingEvent.class));

        assertThrows(RuntimeException.class, () -> dispatchService.process(testKey, testOrderCreatedEvent));
        verify(clientMock).checkAvailability(testOrderCreatedEvent.getItem());
        verify(kafkaTemplateMock).send(eq(ORDER_DISPATCHED_TOPIC), eq(testKey), any(OrderDispatchedEvent.class));
        verify(kafkaTemplateMock).send(eq(DISPATCH_TRACKING_TOPIC), eq(testKey), any(DispatchPreparingEvent.class));
        verifyNoMoreInteractions(kafkaTemplateMock);
    }

    @Test
    void process_throwsExceptionOnDispatchCompletedEvent() {
        when(clientMock.checkAvailability(anyString())).thenReturn(Boolean.TRUE.toString());
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparingEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchCompletedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        doThrow(new RuntimeException()).when(kafkaTemplateMock).send(anyString(), anyString(), any(DispatchCompletedEvent.class));

        assertThrows(RuntimeException.class, () -> dispatchService.process(testKey, testOrderCreatedEvent));
        verify(clientMock).checkAvailability(testOrderCreatedEvent.getItem());
        verify(kafkaTemplateMock).send(eq(ORDER_DISPATCHED_TOPIC), eq(testKey), any(OrderDispatchedEvent.class));
        verify(kafkaTemplateMock).send(eq(DISPATCH_TRACKING_TOPIC), eq(testKey), any(DispatchPreparingEvent.class));
        verify(kafkaTemplateMock).send(eq(DISPATCH_TRACKING_TOPIC), eq(testKey), any(DispatchCompletedEvent.class));
    }
}
