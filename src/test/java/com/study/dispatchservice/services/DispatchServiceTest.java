package com.study.dispatchservice.services;

import com.study.dispatchservice.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DispatchServiceTest {

    private DispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new DispatchService();
    }

    @Test
    void process() {
        var testEvent = EventUtils.randomOrderCreatedEvent();
        dispatchService.process(testEvent);
    }
}