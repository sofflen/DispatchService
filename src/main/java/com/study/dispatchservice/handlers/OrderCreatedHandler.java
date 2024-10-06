package com.study.dispatchservice.handlers;

import com.study.dispatchservice.exceptions.NotRetryableException;
import com.study.dispatchservice.exceptions.RetryableException;
import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.services.DispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedHandler {

    public static final String ORDER_CREATED_TOPIC = "order.created";

    private final DispatchService dispatchService;

    @KafkaListener(
            id = "orderConsumerClient",
            topics = ORDER_CREATED_TOPIC,
            groupId = "dispatch.order.created.consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                       @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                       @Payload OrderCreatedEvent payload) {
        log.info("OrderCreatedHandler received message - partition: {}, key: {}, payload: {}",
                partition, key, payload);
        try {
            dispatchService.process(key, payload);
        } catch (RetryableException e) {
            log.warn("RetryableException occurred while processing key: {}, payload: {}, exception message: {}",
                    key, payload, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("NotRetryableException occurred while processing key: {}, payload: {}, exception message: {}",
                    key, payload, e.getMessage());
            if (e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new NotRetryableException(e);
        }
    }
}
