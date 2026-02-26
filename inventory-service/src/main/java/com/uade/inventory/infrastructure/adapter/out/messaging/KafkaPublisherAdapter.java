package com.uade.inventory.infrastructure.adapter.out.messaging;

import com.uade.inventory.domain.event.ProductCreatedEvent;
import com.uade.inventory.domain.port.out.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
public class KafkaPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaPublisherAdapter.class);

    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    public KafkaPublisherAdapter(KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishProductCreated(ProductCreatedEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC, String.valueOf(event.getProductId()), event);
        log.info("Evento publicado en Kafka: ProductCreated [id={}, name={}]", event.getProductId(), event.getName());
    }
}
