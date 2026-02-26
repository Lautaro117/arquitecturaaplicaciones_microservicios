package com.uade.inventory.domain.port.out;

import com.uade.inventory.domain.event.ProductCreatedEvent;

public interface EventPublisherPort {

    void publishProductCreated(ProductCreatedEvent event);
}
