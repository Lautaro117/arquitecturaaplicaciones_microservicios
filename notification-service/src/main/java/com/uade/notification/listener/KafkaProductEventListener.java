package com.uade.notification.listener;

import com.uade.notification.event.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("kafka")
public class KafkaProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaProductEventListener.class);

    @KafkaListener(topics = "product-created", groupId = "notification-group")
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("=== NOTIFICACIÓN RECIBIDA (Kafka) ===");
        log.info("Nuevo producto creado:");
        log.info("  ID:       {}", event.getProductId());
        log.info("  Nombre:   {}", event.getName());
        log.info("  Cantidad: {}", event.getQuantity());
        log.info("  Precio:   ${}", event.getPrice());
        log.info("  Fecha:    {}", event.getTimestamp());
        log.info("=====================================");
    }
}
