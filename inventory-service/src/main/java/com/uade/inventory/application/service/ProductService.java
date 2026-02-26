package com.uade.inventory.application.service;

import com.uade.inventory.domain.event.ProductCreatedEvent;
import com.uade.inventory.domain.model.Product;
import com.uade.inventory.domain.port.in.ProductUseCase;
import com.uade.inventory.domain.port.out.EventPublisherPort;
import com.uade.inventory.domain.port.out.ProductRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService implements ProductUseCase {

    private final ProductRepositoryPort repositoryPort;
    private final EventPublisherPort eventPublisherPort;

    public ProductService(ProductRepositoryPort repositoryPort, EventPublisherPort eventPublisherPort) {
        this.repositoryPort = repositoryPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    public List<Product> getAllProducts() {
        return repositoryPort.findAll();
    }

    @Override
    public Optional<Product> getProductById(Long id) {
        return repositoryPort.findById(id);
    }

    @Override
    public Product createProduct(Product product) {
        Product saved = repositoryPort.save(product);
        eventPublisherPort.publishProductCreated(
                new ProductCreatedEvent(saved.getId(), saved.getName(), saved.getQuantity(), saved.getPrice())
        );
        return saved;
    }
}
