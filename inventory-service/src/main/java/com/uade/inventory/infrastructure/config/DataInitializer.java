package com.uade.inventory.infrastructure.config;

import com.uade.inventory.domain.model.Product;
import com.uade.inventory.domain.port.out.ProductRepositoryPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepositoryPort productRepositoryPort;

    public DataInitializer(ProductRepositoryPort productRepositoryPort) {
        this.productRepositoryPort = productRepositoryPort;
    }

    @Override
    public void run(String... args) {
        if (productRepositoryPort.count() == 0) {
            productRepositoryPort.save(new Product("Laptop", 10, 999.99));
            productRepositoryPort.save(new Product("Mouse", 50, 29.99));
            productRepositoryPort.save(new Product("Teclado", 30, 79.99));
        }
    }
}
