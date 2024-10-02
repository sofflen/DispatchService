package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.OrderDispatchedEvent;
import com.study.dispatchservice.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

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

    @BeforeEach
    void setUp() {
        kafkaTemplateMock = mock(KafkaTemplate.class);
        dispatchService = new DispatchService(kafkaTemplateMock);
    }

    @Test
    void process_success() throws Exception {
        when(kafkaTemplateMock.send(anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));

        var testEvent = EventUtils.randomOrderCreatedEvent();
        dispatchService.process(testEvent);

        verify(kafkaTemplateMock).send(eq("order.dispatched"), any(OrderDispatchedEvent.class));
    }

    @Test
    void process_throwsException() {
        when(kafkaTemplateMock.send(anyString(), any(OrderDispatchedEvent.class)))
                .thenReturn(mock(CompletableFuture.class));
        doThrow(new RuntimeException()).when(kafkaTemplateMock).send(anyString(), any(OrderDispatchedEvent.class));

        var testEvent = EventUtils.randomOrderCreatedEvent();

        assertThrows(RuntimeException.class, () -> dispatchService.process(testEvent));
        verify(kafkaTemplateMock).send(eq("order.dispatched"), any(OrderDispatchedEvent.class));
    }
}