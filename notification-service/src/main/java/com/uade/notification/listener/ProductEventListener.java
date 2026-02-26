package com.uade.notification.listener;

import com.uade.notification.event.ProductCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("rabbitmq")
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    @RabbitListener(queues = "product.created.queue")
    public void handleProductCreated(ProductCreatedEvent event) {
        log.info("=== NOTIFICACIÓN RECIBIDA ===");
        log.info("Nuevo producto creado:");
        log.info("  ID:       {}", event.getProductId());
        log.info("  Nombre:   {}", event.getName());
        log.info("  Cantidad: {}", event.getQuantity());
        log.info("  Precio:   ${}", event.getPrice());
        log.info("  Fecha:    {}", event.getTimestamp());
        log.info("=============================");
    }
}
