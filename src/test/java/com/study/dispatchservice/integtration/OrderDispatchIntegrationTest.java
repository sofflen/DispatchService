package com.study.dispatchservice.integtration;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.study.dispatchservice.messages.DispatchCompletedEvent;
import com.study.dispatchservice.messages.DispatchPreparingEvent;
import com.study.dispatchservice.messages.OrderCreatedEvent;
import com.study.dispatchservice.messages.OrderDispatchedEvent;
import com.study.dispatchservice.utils.EventUtils;
import com.study.dispatchservice.utils.WiremockUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.study.dispatchservice.handlers.OrderCreatedHandler.ORDER_CREATED_TOPIC;
import static com.study.dispatchservice.services.DispatchService.DISPATCH_TRACKING_TOPIC;
import static com.study.dispatchservice.services.DispatchService.ORDER_DISPATCHED_TOPIC;
import static com.study.dispatchservice.utils.WiremockUtils.stubWiremock;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
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

    private String testKey;
    private OrderCreatedEvent testOrderCreatedEvent;

    @BeforeEach
    void setUp() {
        testKey = UUID.randomUUID().toString();
        testOrderCreatedEvent = EventUtils.randomOrderCreatedEvent();

        testListener.orderDispatchCounter.set(0);
        testListener.dispatchPreparingCounter.set(0);
        testListener.dispatchCompletedCounter.set(0);

        WiremockUtils.reset();

        registry.getListenerContainers().forEach(container ->
                ContainerTestUtils.waitForAssignment(container,
                        container.getContainerProperties().getTopics().length * embeddedKafkaBroker.getPartitionsPerTopic()));
    }

    @Test
    void testOrderDispatch_Success() throws Exception {
        stubWiremock("/api/stock?item=" + testOrderCreatedEvent.getItem(), 200, "true");

        kafkaTemplate.send(ORDER_CREATED_TOPIC, testKey, testOrderCreatedEvent).get();

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
    }


    @Test
    void testOrderDispatch_NotRetryableException() throws Exception {
        stubWiremock("/api/stock?item=" + testOrderCreatedEvent.getItem(), 400, "Bad Request");

        kafkaTemplate.send(ORDER_CREATED_TOPIC, testKey, testOrderCreatedEvent).get();

        await().during(3, TimeUnit.SECONDS)
                .atMost(4, TimeUnit.SECONDS)
                .until(() -> testListener.orderDispatchCounter.get() == 0
                        && testListener.dispatchPreparingCounter.get() == 0
                        && testListener.dispatchCompletedCounter.get() == 0);

        assertThat(testListener.orderDispatchCounter.get(), equalTo(0));
        assertThat(testListener.dispatchPreparingCounter.get(), equalTo(0));
        assertThat(testListener.dispatchCompletedCounter.get(), equalTo(0));
    }

    @Test
    void testOrderDispatch_RetryThenSuccess() throws Exception {
        stubWiremock("/api/stock?item=" + testOrderCreatedEvent.getItem(), 503, "Service Unavailable",
                "Fail Once", Scenario.STARTED,"Succeed next time");
        stubWiremock("/api/stock?item=" + testOrderCreatedEvent.getItem(), 200, "true",
                "Fail Once", "Succeed next time","Succeed next time");

        kafkaTemplate.send(ORDER_CREATED_TOPIC, testKey, testOrderCreatedEvent).get();

        await().atMost(3, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.orderDispatchCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchPreparingCounter::get, equalTo(1));
        await().atMost(1, TimeUnit.SECONDS).pollDelay(100, TimeUnit.MILLISECONDS)
                .until(testListener.dispatchCompletedCounter::get, equalTo(1));
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
    @KafkaListener(groupId = "KafkaIntegrationTest", topics = {ORDER_DISPATCHED_TOPIC, DISPATCH_TRACKING_TOPIC})
    static class KafkaTestListener {
        AtomicInteger orderDispatchCounter = new AtomicInteger();
        AtomicInteger dispatchPreparingCounter = new AtomicInteger();
        AtomicInteger dispatchCompletedCounter = new AtomicInteger();

        @KafkaHandler
        void receiveOrderDispatchedEvent(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                         @Payload OrderDispatchedEvent payload) {
            log.info("Test: Received order dispatched event: {}, key: {}", payload, key);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            orderDispatchCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchPreparingEvent(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                           @Payload DispatchPreparingEvent payload) {
            log.info("Test: Received dispatch preparing event: {}, key: {}", payload, key);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchPreparingCounter.incrementAndGet();
        }

        @KafkaHandler
        void receiveDispatchCompletedEvent(@Header(KafkaHeaders.RECEIVED_KEY) String key,
                                           @Payload DispatchCompletedEvent payload) {
            log.info("Test: Received dispatch completed event: {}, key: {}", payload, key);
            assertThat(key, notNullValue());
            assertThat(payload, notNullValue());
            dispatchCompletedCounter.incrementAndGet();
        }
    }
}
