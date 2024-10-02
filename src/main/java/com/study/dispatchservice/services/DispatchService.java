package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(OrderCreatedEvent orderCreatedEvent) throws Exception {
        var orderDispatchedEvent = new OrderDispatchedEvent(orderCreatedEvent.getOrderId());
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, orderDispatchedEvent).get();
    }
}
