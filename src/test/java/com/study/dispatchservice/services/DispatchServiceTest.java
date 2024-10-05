package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.DispatchPreparingEvent;
import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import com.study.dispatchservice.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchServiceTest {

    private DispatchService dispatchService;
    private KafkaTemplate<String, Object> kafkaTemplateMock;
    private String testKey;
    private OrderCreatedEvent testOrderCreatedEvent;

    @BeforeEach
    void setUp() {
        kafkaTemplateMock = mock(KafkaTemplate.class);
        dispatchService = new DispatchService(kafkaTemplateMock);
        testKey = UUID.randomUUID().toString();
        testOrderCreatedEvent = EventUtils.randomOrderCreatedEvent();
    }

    @Test
    void process_success() throws Exception {
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparingEvent.class)))
                .thenReturn(mock(CompletableFuture.class));

        dispatchService.process(testKey, testOrderCreatedEvent);

        verify(kafkaTemplateMock).send(eq("order.dispatched"), eq(testKey), any(OrderDispatchedEvent.class));
        verify(kafkaTemplateMock).send(eq("dispatch.tracking"), eq(testKey), any(DispatchPreparingEvent.class));
    }

    @Test
    void process_throwsExceptionOnDispatchPreparingEvent() {
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparingEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        doThrow(new RuntimeException()).when(kafkaTemplateMock).send(anyString(), anyString(), any(DispatchPreparingEvent.class));

        assertThrows(RuntimeException.class, () -> dispatchService.process(testKey, testOrderCreatedEvent));
        verify(kafkaTemplateMock).send(eq("order.dispatched"), eq(testKey), any(OrderDispatchedEvent.class));
        verify(kafkaTemplateMock).send(eq("dispatch.tracking"), eq(testKey), any(DispatchPreparingEvent.class));
    }

    @Test
    void process_throwsExceptionOnOrderDispatchedEvent() {
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        doThrow(new RuntimeException()).when(kafkaTemplateMock).send(anyString(), anyString(), any(OrderDispatchedEvent.class));

        assertThrows(RuntimeException.class, () -> dispatchService.process(testKey, testOrderCreatedEvent));
        verify(kafkaTemplateMock).send(eq("order.dispatched"), eq(testKey), any(OrderDispatchedEvent.class));
    }
}
