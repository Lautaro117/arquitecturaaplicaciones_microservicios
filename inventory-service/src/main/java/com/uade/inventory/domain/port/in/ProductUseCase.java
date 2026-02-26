package com.uade.inventory.domain.port.in;

import com.uade.inventory.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductUseCase {

    List<Product> getAllProducts();

    Optional<Product> getProductById(Long id);

    Product createProduct(Product product);
}
