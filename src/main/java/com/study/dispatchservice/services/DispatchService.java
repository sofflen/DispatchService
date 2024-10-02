package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(OrderCreatedEvent orderCreatedEvent) throws Exception {
        log.info("Processing order dispatched event: {}", orderCreatedEvent);
        var orderDispatchedEvent = new OrderDispatchedEvent(orderCreatedEvent.getOrderId());
        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, orderDispatchedEvent).get();
        log.info("Order dispatched event sent: {}", orderDispatchedEvent);
    }
}
