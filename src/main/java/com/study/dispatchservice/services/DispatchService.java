package com.study.dispatchservice.services;

import com.study.dispatchservice.messages.DispatchCompletedEvent;
import com.study.dispatchservice.messages.DispatchPreparingEvent;
import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        var dispatchPreparingEvent = new DispatchPreparingEvent(orderId);

        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparingEvent).get();
        log.info("Dispatch tracking event sent: {}, key: {}", dispatchPreparingEvent, key);

        //To mimic some business logic before dispatch is completed
        Thread.sleep(1000);

        var dispatchCompletedEvent = DispatchCompletedEvent.builder()
                .orderId(orderId)
                .date(LocalDateTime.now().toString())
                .build();

        kafkaProducer.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompletedEvent).get();
        log.info("Dispatch is completed: {}, key: {}", dispatchCompletedEvent, key);
    }
}
