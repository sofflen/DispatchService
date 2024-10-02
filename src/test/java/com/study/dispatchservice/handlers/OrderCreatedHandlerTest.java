package com.study.dispatchservice.handlers;

import com.study.dispatchservice.services.DispatchService;
import com.study.dispatchservice.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void listen() {
        var testEvent = EventUtils.randomOrderCreatedEvent();

        orderCreatedHandler.listen(testEvent);

        verify(dispatchServiceMock).process(testEvent);
    }
}