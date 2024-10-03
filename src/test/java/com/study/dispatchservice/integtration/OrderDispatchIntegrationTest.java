package com.study.dispatchservice.integtration;

import com.study.dispatchservice.messages.DispatchPreparingEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import com.study.dispatchservice.utils.EventUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.study.dispatchservice.handlers.OrderCreatedHandler.ORDER_CREATED_TOPIC;
import static com.study.dispatchservice.services.DispatchService.DISPATCH_TRACKING_TOPIC;
import static com.study.dispatchservice.services.DispatchService.ORDER_DISPATCHED_TOPIC;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@EmbeddedKafka(controlledShutdown = true)
@Slf4j
class OrderDispatchIntegrationTest {

    @Autowired
    private KafkaTestListener testListener;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void setUp() {
        testListener.orderDispatchCounter.set(0);
        testListener.dispatchPreparingCounter.set(0);

        registry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    void testOrderDispatch() throws Exception {
        var orderCreatedEvent = EventUtils.randomOrderCreatedEvent();

        kafkaTemplate.send(ORDER_CREATED_TOPIC, orderCreatedEvent).get();

        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchCounter::get, equalTo(1));
        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        public KafkaTestListener kafkaTestListener() {
            return new KafkaTestListener();
        }
    }

    /**
     * Use this receiver to consume message from the outbound topics
     */
    static class KafkaTestListener {
        AtomicInteger dispatchPreparingCounter = new AtomicInteger();
        AtomicInteger orderDispatchCounter = new AtomicInteger();

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = ORDER_DISPATCHED_TOPIC)
        void receiveOrderDispatchedEvent(@Payload OrderDispatchedEvent payload) {
            log.info("Test: Received order dispatched event: {}", payload);
            orderDispatchCounter.incrementAndGet();
        }

        @KafkaListener(groupId = "KafkaIntegrationTest", topics = DISPATCH_TRACKING_TOPIC)
        void receiveDispatchPreparingEvent(@Payload DispatchPreparingEvent payload) {
            log.info("Test: Received dispatch preparing event: {}", payload);
            dispatchPreparingCounter.incrementAndGet();
        }
    }
}
