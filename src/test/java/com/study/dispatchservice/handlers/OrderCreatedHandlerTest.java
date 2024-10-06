package com.study.dispatchservice.handlers;

import com.study.dispatchservice.exceptions.NotRetryableException;
import com.study.dispatchservice.exceptions.RetryableException;
import com.study.dispatchservice.services.DispatchService;
import com.study.dispatchservice.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderCreatedHandlerTest {

    private OrderCreatedHandler orderCreatedHandler;
    private DispatchService dispatchServiceMock;

    @BeforeEach
    void setUp() {
        dispatchServiceMock = mock(DispatchService.class);
        orderCreatedHandler = new OrderCreatedHandler(dispatchServiceMock);
    }

    @Test
    void listen_success() throws Exception {
        String key = UUID.randomUUID().toString();
        var testEvent = EventUtils.randomOrderCreatedEvent();

        orderCreatedHandler.listen(0, key, testEvent);

        verify(dispatchServiceMock).process(key, testEvent);
    }

    @Test
    void listen_RetryableException() throws Exception {
        String key = UUID.randomUUID().toString();
        var testEvent = EventUtils.randomOrderCreatedEvent();
        doThrow(new RetryableException()).when(dispatchServiceMock).process(key, testEvent);

        assertThrows(RetryableException.class, () -> orderCreatedHandler.listen(0, key, testEvent));

        verify(dispatchServiceMock).process(key, testEvent);
    }

    @Test
    void listen_NotRetryableException() throws Exception {
        String key = UUID.randomUUID().toString();
        var testEvent = EventUtils.randomOrderCreatedEvent();
        doThrow(new RuntimeException()).when(dispatchServiceMock).process(key, testEvent);

        assertThrows(NotRetryableException.class, () -> orderCreatedHandler.listen(0, key, testEvent));

        verify(dispatchServiceMock).process(key, testEvent);
    }
}