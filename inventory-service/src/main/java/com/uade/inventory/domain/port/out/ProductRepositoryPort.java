package com.uade.inventory.domain.port.out;

import com.uade.inventory.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {

    List<Product> findAll();

    Optional<Product> findById(Long id);

    Product save(Product product);

    long count();
}
