package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.DispatchPreparingEvent;
import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    public static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    public static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    public static final UUID APPLICATION_ID = UUID.randomUUID();

    private final KafkaTemplate<String, Object> kafkaProducer;

    public void process(String key, OrderCreatedEvent orderCreatedEvent) throws Exception {
        log.info("Processing order created event: {}", orderCreatedEvent);

        var orderId = orderCreatedEvent.getOrderId();
        var orderDispatchedEvent = OrderDispatchedEvent.builder()
                .orderId(orderId)
                .processedById(APPLICATION_ID)
                .notes("Dispatched: " + orderCreatedEvent.getItem())
                .build();

        kafkaProducer.send(ORDER_DISPATCHED_TOPIC, key, orderDispatchedEvent).get();
        log.info("Order dispatched event sent: {}, key: {} - processed by id: {}",
                orderDispatchedEvent, key, APPLICATION_ID);

        var dispatchTrackingEvent = new DispatchPreparingEvent(orderId);

        log.info("Preparing dispatch: {}", dispatchTrackingEvent);
        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchTrackingEvent).get();
        log.info("Dispatch tracking event sent: {}, key: {}", dispatchTrackingEvent, key);
    }
}
