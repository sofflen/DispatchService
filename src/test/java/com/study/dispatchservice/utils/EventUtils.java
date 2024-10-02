package com.study.dispatchservice.utils;

import com.study.dispatchservice.messages.OrderCreatedEvent;

import java.util.UUID;

public class EventUtils {

    public static OrderCreatedEvent randomOrderCreatedEvent() {
        return OrderCreatedEvent.builder()
                .orderId(UUID.randomUUID())
                .item(UUID.randomUUID().toString())
                .build();
    }
}
